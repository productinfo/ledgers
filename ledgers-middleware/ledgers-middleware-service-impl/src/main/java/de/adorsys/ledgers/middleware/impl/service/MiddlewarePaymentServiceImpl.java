/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.ledgers.middleware.impl.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.adorsys.ledgers.deposit.api.domain.PaymentBO;
import de.adorsys.ledgers.deposit.api.domain.TransactionStatusBO;
import de.adorsys.ledgers.deposit.api.exception.DepositAccountNotFoundException;
import de.adorsys.ledgers.deposit.api.exception.PaymentNotFoundException;
import de.adorsys.ledgers.deposit.api.service.DepositAccountPaymentService;
import de.adorsys.ledgers.deposit.api.service.DepositAccountService;
import de.adorsys.ledgers.middleware.api.domain.payment.PaymentCoreDataTO;
import de.adorsys.ledgers.middleware.api.domain.payment.PaymentTypeTO;
import de.adorsys.ledgers.middleware.api.domain.payment.TransactionStatusTO;
import de.adorsys.ledgers.middleware.api.domain.sca.SCAPaymentResponseTO;
import de.adorsys.ledgers.middleware.api.domain.sca.ScaStatusTO;
import de.adorsys.ledgers.middleware.api.domain.um.AccessTokenTO;
import de.adorsys.ledgers.middleware.api.domain.um.BearerTokenTO;
import de.adorsys.ledgers.middleware.api.domain.um.ScaUserDataTO;
import de.adorsys.ledgers.middleware.api.domain.um.UserTO;
import de.adorsys.ledgers.middleware.api.exception.AccountMiddlewareUncheckedException;
import de.adorsys.ledgers.middleware.api.exception.AccountNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.api.exception.NoAccessMiddlewareException;
import de.adorsys.ledgers.middleware.api.exception.PaymentNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.api.exception.PaymentProcessingMiddlewareException;
import de.adorsys.ledgers.middleware.api.exception.SCAMethodNotSupportedMiddleException;
import de.adorsys.ledgers.middleware.api.exception.SCAOperationExpiredMiddlewareException;
import de.adorsys.ledgers.middleware.api.exception.SCAOperationNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.api.exception.SCAOperationUsedOrStolenMiddlewareException;
import de.adorsys.ledgers.middleware.api.exception.SCAOperationValidationMiddlewareException;
import de.adorsys.ledgers.middleware.api.exception.UserScaDataNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.api.service.MiddlewarePaymentService;
import de.adorsys.ledgers.middleware.impl.converter.BearerTokenMapper;
import de.adorsys.ledgers.middleware.impl.converter.PaymentConverter;
import de.adorsys.ledgers.middleware.impl.policies.PaymentCancelPolicy;
import de.adorsys.ledgers.middleware.impl.policies.PaymentCoreDataPolicy;
import de.adorsys.ledgers.sca.domain.AuthCodeDataBO;
import de.adorsys.ledgers.sca.domain.OpTypeBO;
import de.adorsys.ledgers.sca.domain.SCAOperationBO;
import de.adorsys.ledgers.sca.domain.ScaStatusBO;
import de.adorsys.ledgers.sca.exception.SCAMethodNotSupportedException;
import de.adorsys.ledgers.sca.exception.SCAOperationExpiredException;
import de.adorsys.ledgers.sca.exception.SCAOperationNotFoundException;
import de.adorsys.ledgers.sca.exception.SCAOperationUsedOrStolenException;
import de.adorsys.ledgers.sca.exception.SCAOperationValidationException;
import de.adorsys.ledgers.sca.service.SCAOperationService;
import de.adorsys.ledgers.um.api.domain.AisAccountAccessInfoBO;
import de.adorsys.ledgers.um.api.domain.AisConsentBO;
import de.adorsys.ledgers.um.api.domain.UserBO;
import de.adorsys.ledgers.um.api.exception.InsufficientPermissionException;
import de.adorsys.ledgers.um.api.exception.UserScaDataNotFoundException;
import de.adorsys.ledgers.um.api.service.UserService;

@Service
public class MiddlewarePaymentServiceImpl implements MiddlewarePaymentService {
	private static final String PAYMENT_WITH_ID_S_NOT_FOUND = "Payment with id %s, not found";
	private static final Logger logger = LoggerFactory.getLogger(MiddlewarePaymentServiceImpl.class);

	private final DepositAccountPaymentService paymentService;
	private final SCAOperationService scaOperationService;
	private final DepositAccountService accountService;
	private final PaymentConverter paymentConverter;
	private final AccessTokenTO accessTokenTO;
	private final BearerTokenMapper bearerTokenMapper;
	private final UserService userService;
	private final SCAUtils scaUtils;
	private final PaymentCancelPolicy cancelPolicy;
	private final PaymentCoreDataPolicy coreDataPolicy;

	public MiddlewarePaymentServiceImpl(DepositAccountPaymentService paymentService,
			SCAOperationService scaOperationService, DepositAccountService accountService,
			PaymentConverter paymentConverter, AccessTokenTO accessTokenTO, BearerTokenMapper bearerTokenMapper,
			UserService userService, SCAUtils scaUtils, PaymentCancelPolicy cancelPolicy,
			PaymentCoreDataPolicy keyDataPolicy) {
		super();
		this.paymentService = paymentService;
		this.scaOperationService = scaOperationService;
		this.accountService = accountService;
		this.paymentConverter = paymentConverter;
		this.accessTokenTO = accessTokenTO;
		this.bearerTokenMapper = bearerTokenMapper;
		this.userService = userService;
		this.scaUtils = scaUtils;
		this.cancelPolicy = cancelPolicy;
		this.coreDataPolicy = keyDataPolicy;
	}

	@Override
	public TransactionStatusTO getPaymentStatusById(String paymentId) throws PaymentNotFoundMiddlewareException {
		try {
			TransactionStatusBO paymentStatus = paymentService.getPaymentStatusById(paymentId);
			return TransactionStatusTO.valueOf(paymentStatus.name());
		} catch (PaymentNotFoundException e) {
			logger.error("Payment with id=" + paymentId + " not found", e);
			throw new PaymentNotFoundMiddlewareException(e.getMessage(), e);
		}
	}

	@Override
	public SCAPaymentResponseTO initiatePaymentCancellation(String paymentId)
			throws PaymentNotFoundMiddlewareException, PaymentProcessingMiddlewareException {
		UserBO userBO = scaUtils.userBO();
		PaymentBO paymentBO = payment(paymentId);
		TransactionStatusTO originalTxStatus = TransactionStatusTO.valueOf(paymentBO.getTransactionStatus().name());
		cancelPolicy.onCancel(paymentId, originalTxStatus);

		PaymentCoreDataTO paymentKeyData = coreDataPolicy.getCancelPaymentCoreData(paymentBO);
		SCAPaymentResponseTO response = prepareSCA(userBO, paymentBO, paymentKeyData, OpTypeBO.CANCEL_PAYMENT);
		
		// If exempted, execute.
		if(ScaStatusTO.EXEMPTED.equals(response.getScaStatus())) {
			try {
				TransactionStatusBO cancelPayment = paymentService.cancelPayment(paymentId);
				response.setTransactionStatus(TransactionStatusTO.valueOf(cancelPayment.name()));
			} catch (PaymentNotFoundException e) {
				// SHall not happen
				throw new AccountMiddlewareUncheckedException(e.getMessage(), e);
			}
		} else {
			response.setTransactionStatus(TransactionStatusTO.valueOf(paymentBO.getTransactionStatus().name()));
		}

		return response;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> SCAPaymentResponseTO initiatePayment(T payment, PaymentTypeTO paymentType)
			throws AccountNotFoundMiddlewareException, NoAccessMiddlewareException {
		PaymentBO paymentBO = paymentConverter.toPaymentBO(payment, paymentType.getPaymentClass());
		UserBO userBO = scaUtils.userBO();
		checkDepositAccount(paymentBO);
		
		TransactionStatusBO status = scaUtils.hasSCA(userBO) 
				? TransactionStatusBO.ACCP
				: TransactionStatusBO.ACTC;

		paymentBO = paymentService.initiatePayment(paymentBO, status);
		status = paymentBO.getTransactionStatus();
		SCAPaymentResponseTO response = new SCAPaymentResponseTO();
		if (TransactionStatusBO.RJCT.equals(status)) {
			response.setScaStatus(ScaStatusTO.FAILED);
			response.setTransactionStatus(TransactionStatusTO.valueOf(status.name()));
			response.setPaymentId(paymentBO.getPaymentId());
		} else {
			PaymentCoreDataTO paymentKeyData = coreDataPolicy.getPaymentCoreData(paymentBO);
			response = prepareSCA(userBO, paymentBO, paymentKeyData, OpTypeBO.PAYMENT);
			if(ScaStatusTO.EXEMPTED.equals(response.getScaStatus())) {
				try {
					TransactionStatusBO tx = paymentService.updatePaymentStatusToAuthorised(paymentBO.getPaymentId());
					response.setTransactionStatus(TransactionStatusTO.valueOf(tx.name()));
					BearerTokenTO paymentAccountAccessToken = paymentAccountAccessToken(paymentBO);
					response.setBearerToken(paymentAccountAccessToken);
				} catch (PaymentNotFoundException e) {
					// SHall not happen
					throw new AccountMiddlewareUncheckedException(e.getMessage(), e);
				}
			}
		}
		return response;
	}

	private void checkDepositAccount(PaymentBO paymentBO) throws AccountNotFoundMiddlewareException {
		try {
			accountService.getDepositAccountByIban(paymentBO.getDebtorAccount().getIban(), LocalDateTime.now(), false);

		} catch (DepositAccountNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new AccountNotFoundMiddlewareException(e.getMessage());
		}
	}

	@Override
	public Object getPaymentById(String paymentId) throws PaymentNotFoundMiddlewareException {
		try {
			PaymentBO paymentResult = paymentService.getPaymentById(paymentId);
			return paymentConverter.toPaymentTO(paymentResult);
		} catch (PaymentNotFoundException e) {
			logger.error(String.format("Payment with id= %s, not found", paymentId), e);
			throw new PaymentNotFoundMiddlewareException(e.getMessage(), e);
		}
	}

	@Override
	public String iban(String paymentId) {
		return paymentService.readIbanByPaymentId(paymentId);
	}

	/*
	 * Authorizes a payment. Schedule the payment for execution if no further authorization is required.
	 * 
	 */
	@Override
	public SCAPaymentResponseTO authorizePayment(String paymentId, String authorisationId, String authCode)
			throws SCAOperationNotFoundMiddlewareException, SCAOperationValidationMiddlewareException,
			SCAOperationExpiredMiddlewareException, SCAOperationUsedOrStolenMiddlewareException, 
			PaymentNotFoundMiddlewareException {
		
		PaymentBO payment = payment(paymentId);
		PaymentCoreDataTO paymentKeyData = coreDataPolicy.getPaymentCoreData(payment);
		try {
			validateAuthCode(payment, authorisationId, authCode, paymentKeyData.template());
			if(scaOperationService.authenticationCompleted(paymentId, OpTypeBO.PAYMENT)) {
				paymentService.updatePaymentStatusToAuthorised(paymentId);
			}
			BearerTokenTO bearerToken = paymentAccountAccessToken(payment);
			SCAPaymentResponseTO response = toScaPaymentResponse(scaUtils.user(), payment, paymentKeyData, scaUtils.loadAuthCode(authorisationId));
			response.setBearerToken(bearerToken);
			return response;
		} catch (PaymentNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new AccountMiddlewareUncheckedException(e.getMessage(), e);
		}
	}

	private BearerTokenTO paymentAccountAccessToken(PaymentBO payment) {
		String iban = payment.getDebtorAccount().getIban();
		// Returned token can be used to access status.
		try {
			AisConsentBO aisConsent = new AisConsentBO();
			AisAccountAccessInfoBO access = new AisAccountAccessInfoBO();
			aisConsent.setAccess(access);
			List<String> asList = Arrays.asList(iban);
			access.setAccounts(asList);
			access.setTransactions(asList);
			access.setBalances(asList);
			aisConsent.setFrequencyPerDay(0);
			aisConsent.setRecurringIndicator(true);
			aisConsent.setUserId(accessTokenTO.getActor());
			return bearerTokenMapper.toBearerTokenTO(userService.grant(accessTokenTO.getSub(),aisConsent));
		} catch (InsufficientPermissionException e) {
			throw new AccountMiddlewareUncheckedException(e.getMessage(), e);
		}
	}

	@Override
	public SCAPaymentResponseTO loadSCAForPaymentData(String paymentId, String authorisationId)
			throws PaymentNotFoundMiddlewareException, SCAOperationExpiredMiddlewareException {
		SCAOperationBO a;
		try {
			a = scaOperationService.loadAuthCode(authorisationId);
		} catch (SCAOperationNotFoundException e) {
			throw new SCAOperationExpiredMiddlewareException(e.getMessage(), e);
		}
		PaymentBO payment = payment(paymentId);
		PaymentCoreDataTO paymentKeyData = coreDataPolicy.getPaymentCoreData(payment);
		return toScaPaymentResponse(scaUtils.user(), payment, paymentKeyData, a);
	}

	@Override
	public SCAPaymentResponseTO selectSCAMethodForPayment(String paymentId, String authorisationId, String scaMethodId)
			throws PaymentNotFoundMiddlewareException, SCAMethodNotSupportedMiddleException, 
			UserScaDataNotFoundMiddlewareException, SCAOperationValidationMiddlewareException, 
			SCAOperationNotFoundMiddlewareException 
	{
		UserBO userBO = scaUtils.userBO();
		UserTO userTO = scaUtils.user(userBO);
		PaymentBO payment = payment(paymentId);
		PaymentCoreDataTO paymentKeyData = coreDataPolicy.getPaymentCoreData(payment);
		AuthCodeDataBO a = new AuthCodeDataBO();
		a.setAuthorisationId(authorisationId);
		a.setScaUserDataId(scaMethodId);
		a.setOpId(paymentId);
		a.setOpData(paymentKeyData.template());
		a.setUserMessage(paymentKeyData.template());

		try {
			SCAOperationBO scaOperationBO = scaOperationService.generateAuthCode(a, userBO, ScaStatusBO.SCAMETHODSELECTED);
			return toScaPaymentResponse(userTO, payment, paymentKeyData, scaOperationBO);
		} catch (SCAMethodNotSupportedException e) {
			logger.error(e.getMessage(), e);
			throw new SCAMethodNotSupportedMiddleException(e);
		} catch (UserScaDataNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new UserScaDataNotFoundMiddlewareException(e);
		} catch (SCAOperationValidationException e) {
			throw new SCAOperationValidationMiddlewareException(e.getMessage(), e);
		} catch (SCAOperationNotFoundException e) {
			throw new SCAOperationNotFoundMiddlewareException(e.getMessage(), e);
		}
	}

	@Override
	public SCAPaymentResponseTO loadSCAForCancelPaymentData(String paymentId, String cancellationId)
			throws PaymentNotFoundMiddlewareException, SCAOperationExpiredMiddlewareException {
		SCAOperationBO a;
		try {
			a = scaOperationService.loadAuthCode(cancellationId);
		} catch (SCAOperationNotFoundException e) {
			throw new SCAOperationExpiredMiddlewareException(e.getMessage(), e);
		}
		UserTO user = scaUtils.user();
		PaymentBO payment = payment(paymentId);
		PaymentCoreDataTO paymentKeyData = coreDataPolicy.getCancelPaymentCoreData(payment);
		return toScaPaymentResponse(user, payment, paymentKeyData, a);
	}

	@Override
	public SCAPaymentResponseTO selectSCAMethodForCancelPayment(String paymentId, String cancellationId,
			String scaMethodId)
					throws PaymentNotFoundMiddlewareException, SCAMethodNotSupportedMiddleException, 
					UserScaDataNotFoundMiddlewareException, SCAOperationValidationMiddlewareException, 
					SCAOperationNotFoundMiddlewareException 
			{
		UserBO userBO = scaUtils.userBO();
		UserTO userTO = scaUtils.user(userBO);
		PaymentBO payment = payment(paymentId);
		PaymentCoreDataTO paymentKeyData = coreDataPolicy.getCancelPaymentCoreData(payment);
		AuthCodeDataBO a = new AuthCodeDataBO();
		a.setAuthorisationId(cancellationId);
		a.setScaUserDataId(scaMethodId);
		a.setOpId(paymentId);
		a.setOpData(paymentKeyData.template());
		a.setUserMessage(paymentKeyData.template());

		try {
			SCAOperationBO scaOperationBO = scaOperationService.generateAuthCode(a, userBO, ScaStatusBO.SCAMETHODSELECTED);
			return toScaPaymentResponse(userTO, payment, paymentKeyData, scaOperationBO);
		} catch (SCAMethodNotSupportedException e) {
			logger.error(e.getMessage(), e);
			throw new SCAMethodNotSupportedMiddleException(e);
		} catch (UserScaDataNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new UserScaDataNotFoundMiddlewareException(e);
		} catch (SCAOperationValidationException e) {
			throw new SCAOperationValidationMiddlewareException(e.getMessage(), e);
		} catch (SCAOperationNotFoundException e) {
			throw new SCAOperationNotFoundMiddlewareException(e.getMessage(), e);
		}
	}

	@Override
	public SCAPaymentResponseTO authorizeCancelPayment(String paymentId, String cancellationId, String authCode)
			throws SCAOperationNotFoundMiddlewareException, SCAOperationValidationMiddlewareException,
			SCAOperationExpiredMiddlewareException, SCAOperationUsedOrStolenMiddlewareException, PaymentNotFoundMiddlewareException {
		PaymentBO payment = payment(paymentId);
		PaymentCoreDataTO paymentKeyData = coreDataPolicy.getCancelPaymentCoreData(payment);
		try {
			validateAuthCode(payment, cancellationId, authCode, paymentKeyData.template());
			if(scaOperationService.authenticationCompleted(paymentId, OpTypeBO.CANCEL_PAYMENT)) {
				paymentService.cancelPayment(paymentId);
			}
			SCAPaymentResponseTO response = toScaPaymentResponse(scaUtils.user(), payment, paymentKeyData, scaUtils.loadAuthCode(cancellationId));
			BearerTokenTO bearerToken = paymentAccountAccessToken(payment);
			response.setBearerToken(bearerToken);		
			return response;
		} catch (PaymentNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new AccountMiddlewareUncheckedException(e.getMessage(), e);
		}
	}

	@SuppressWarnings("PMD.CyclomaticComplexity")
	private void validateAuthCode(PaymentBO payment, String authorisationId, String authCode, String template)
			throws SCAOperationNotFoundMiddlewareException, SCAOperationValidationMiddlewareException,
			SCAOperationUsedOrStolenMiddlewareException, SCAOperationExpiredMiddlewareException{
		try {
			if(!scaOperationService.validateAuthCode(authorisationId, payment.getPaymentId(),template, authCode)) {
				throw new SCAOperationValidationMiddlewareException("Wrong auth code");
			}
		} catch (SCAOperationNotFoundException e) {
			throw new SCAOperationNotFoundMiddlewareException(e);
		} catch (SCAOperationValidationException e) {
			throw new SCAOperationValidationMiddlewareException(e);
		} catch (SCAOperationUsedOrStolenException e) {
			throw new SCAOperationUsedOrStolenMiddlewareException(e);
		} catch (SCAOperationExpiredException e) {
			throw new SCAOperationExpiredMiddlewareException(e);
		}
	}

	private PaymentBO payment(String paymentId) throws PaymentNotFoundMiddlewareException {
		try {
			return paymentService.getPaymentById(paymentId);
		} catch (PaymentNotFoundException e) {
			String message = String.format(PAYMENT_WITH_ID_S_NOT_FOUND, paymentId);
			logger.error(message, e);
			throw new PaymentNotFoundMiddlewareException(message, e);
		}
	}

	/*
	 * The SCA requirement shall be added as property of a deposit account permissionning.
	 * 
	 * For now we will assume there is no sca requirement, when the user having access
	 * to the account does not habe any sca data configured.
	 */
	@SuppressWarnings("PMD.UnusedFormalParameter")
	private boolean scaRequired(PaymentBO payment, UserBO user, OpTypeBO opType) {
		return scaUtils.hasSCA(user);
	}

	private SCAPaymentResponseTO prepareSCA(UserBO user, PaymentBO payment, PaymentCoreDataTO paymentKeyData, OpTypeBO opType) {
		SCAPaymentResponseTO response = new SCAPaymentResponseTO();
		String paymentKeyDataTemplate = paymentKeyData.template();
		if (!scaRequired(payment, user, OpTypeBO.CONSENT)) {
			response.setScaStatus(ScaStatusTO.EXEMPTED);
		} else {
			// start SCA
			SCAOperationBO scaOperationBO;
			UserTO userTo = scaUtils.user(user);
			AuthCodeDataBO authCodeData = authCodeData(payment.getPaymentId(), user, opType, paymentKeyDataTemplate);
			if (userTo.getScaUserData().size() == 1) {
				ScaUserDataTO chosenScaMethod = userTo.getScaUserData().iterator().next();
				authCodeData.setScaUserDataId(chosenScaMethod.getId());
				try {
					scaOperationBO = scaOperationService.generateAuthCode(authCodeData, user, ScaStatusBO.SCAMETHODSELECTED);
				} catch (SCAMethodNotSupportedException | UserScaDataNotFoundException | SCAOperationValidationException
						| SCAOperationNotFoundException e) {
					throw new AccountMiddlewareUncheckedException(e.getMessage(), e);
				}
			} else {
				scaOperationBO = scaOperationService.createAuthCode(authCodeData, ScaStatusBO.PSUAUTHENTICATED);
			}
			response = toScaPaymentResponse(userTo, payment, paymentKeyData, scaOperationBO);
		}
		return response;
	}

	private AuthCodeDataBO authCodeData(String paymentId, UserBO user, OpTypeBO opType, String paymentKeyDataTemplate) {
		AuthCodeDataBO authCodeData = new AuthCodeDataBO();
		authCodeData.setOpData(paymentKeyDataTemplate);
		authCodeData.setOpId(paymentId);
		authCodeData.setOpType(opType);
		authCodeData.setUserLogin(user.getLogin());
		authCodeData.setUserMessage(paymentKeyDataTemplate);
		authCodeData.setValiditySeconds(1800);
		return authCodeData;
	}

	private SCAPaymentResponseTO toScaPaymentResponse(UserTO user, PaymentBO payment, PaymentCoreDataTO paymentKeyData,
			SCAOperationBO a) {
		SCAPaymentResponseTO response = new SCAPaymentResponseTO();
		response.setAuthorisationId(a.getId());
		response.setChosenScaMethod(scaUtils.getScaMethod(user, a.getScaMethodId()));
		response.setChallengeData(null);
		response.setExpiresInSeconds(a.getValiditySeconds());
		response.setPaymentId(payment.getPaymentId());
		response.setPsuMessage(paymentKeyData.template());
		response.setScaMethods(user.getScaUserData());
		response.setScaStatus(ScaStatusTO.valueOf(a.getScaStatus().name()));
		response.setStatusDate(a.getStatusTime());
		response.setTransactionStatus(TransactionStatusTO.valueOf(payment.getTransactionStatus().name()));
		return response;
	}
	
}