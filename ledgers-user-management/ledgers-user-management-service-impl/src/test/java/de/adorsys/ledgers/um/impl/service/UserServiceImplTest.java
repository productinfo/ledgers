package de.adorsys.ledgers.um.impl.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import de.adorsys.ledgers.um.api.domain.BearerTokenBO;
import de.adorsys.ledgers.um.api.domain.ScaUserDataBO;
import de.adorsys.ledgers.um.api.domain.UserBO;
import de.adorsys.ledgers.um.api.domain.UserRoleBO;
import de.adorsys.ledgers.um.api.exception.InsufficientPermissionException;
import de.adorsys.ledgers.um.api.exception.UserNotFoundException;
import de.adorsys.ledgers.um.db.domain.ScaUserDataEntity;
import de.adorsys.ledgers.um.db.domain.UserEntity;
import de.adorsys.ledgers.um.db.repository.UserRepository;
import de.adorsys.ledgers.um.impl.converter.UserConverter;
import de.adorsys.ledgers.util.PasswordEnc;
import pro.javatar.commons.reader.ResourceReader;
import pro.javatar.commons.reader.YamlReader;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {

	@InjectMocks
    public UserServiceImpl userService;

    @Mock
    private UserRepository repository;

    @Mock
    private UserConverter converter;
    
    @Mock
    private PasswordEnc passwordEnc;
    
    @Mock
    private HashMacSecretSource secretSource;
    
    @Mock
    private BearerTokenService bearerTokenService;

    private ResourceReader reader = YamlReader.getInstance();

    private static final String USER_ID = "SomeUniqueID";
    private static final String USER_EMAIL = "vne@adorsys.de";
    private static final String USER_LOGIN = "vne";
    private static final String USER_PIN = "12345678";
    private static final String THE_ENCODED_VALUE = "25d55ad283aa400af464c76d713c07ad";
    private static final String USER_NON_EXISTING_LOGIN = "NonExistingLogin";
    private UserEntity userEntity;
    private UserBO userBO;

    @Before
    public void setUp() {
        userEntity = readUserEntity();
        userBO = readUserBO();
    }

    @Test
    public void updateScaData() throws UserNotFoundException, IOException {
        List<ScaUserDataBO> scaUserDataBOS = getScaUserData(ScaUserDataBO.class);
        List<ScaUserDataEntity> scaUserDataEntities = getScaUserData(ScaUserDataEntity.class);

        when(repository.findFirstByLogin(USER_LOGIN)).thenReturn(Optional.ofNullable(userEntity));
        when(converter.toScaUserDataListEntity(scaUserDataBOS)).thenReturn(scaUserDataEntities);
        when(repository.save(userEntity)).thenReturn(userEntity);

        userService.updateScaData(scaUserDataBOS, USER_LOGIN);

        verify(repository, times(1)).findFirstByLogin(USER_LOGIN);
        verify(converter, times(1)).toScaUserDataListEntity(scaUserDataBOS);
        verify(repository, times(1)).save(userEntity);
    }

    @Test(expected = UserNotFoundException.class)
    public void updateScaDataUserNotFound() throws UserNotFoundException, IOException {
        List<ScaUserDataBO> scaUserDataBOS = getScaUserData(ScaUserDataBO.class);

        when(repository.findFirstByLogin(USER_LOGIN)).thenReturn(Optional.empty());

        userService.updateScaData(scaUserDataBOS, USER_LOGIN);
    }

    private <T> List<T> getScaUserData(Class<T> clazz) throws IOException {
        return reader.getListFromInputStream(getClass().getResourceAsStream("sca-user-methods.yml"), clazz);
    }

    @Test(expected=UserNotFoundException.class)
    public void authorizeWithLoginAndPin() throws UserNotFoundException, InsufficientPermissionException {

        when(repository.findFirstByLogin(USER_LOGIN)).thenReturn(Optional.empty());
        BearerTokenBO bearerTokenBO = userService.authorise(USER_LOGIN, USER_PIN, UserRoleBO.CUSTOMER, null, null);

        assertTrue(bearerTokenBO==null);
    }

    @Test
    public void findById() throws UserNotFoundException {

        when(repository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(converter.toUserBO(any())).thenReturn(userBO);
        when(passwordEnc.encode(USER_ID, USER_PIN)).thenReturn(THE_ENCODED_VALUE);
        when(passwordEnc.verify(USER_ID, USER_PIN, THE_ENCODED_VALUE)).thenReturn(true);

        UserBO user = userService.findById(USER_ID);

        assertThat(user.getId(), is(USER_ID));
        assertThat(user.getEmail(), is(USER_EMAIL));
        assertThat(user.getLogin(), is(USER_LOGIN));
        assertTrue(passwordEnc.verify(user.getId(), user.getPin(), passwordEnc.encode(USER_ID, USER_PIN)));

        verify(repository, times(1)).findById(USER_ID);
        verify(converter, times(1)).toUserBO(userEntity);
    }

    @Test(expected = UserNotFoundException.class)
    public void authorizeWithException() throws UserNotFoundException, InsufficientPermissionException {

        when(repository.findFirstByLogin(USER_NON_EXISTING_LOGIN)).thenReturn(Optional.empty());

        userService.authorise(USER_NON_EXISTING_LOGIN, "SomePin", UserRoleBO.CUSTOMER, null, null);
    }

    private UserBO readUserBO() {
        try {
            return reader.getObjectFromResource(getClass(), "user-BO.yml", UserBO.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private UserEntity readUserEntity() {
        try {
            return reader.getObjectFromResource(getClass(), "user-entity.yml", UserEntity.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
