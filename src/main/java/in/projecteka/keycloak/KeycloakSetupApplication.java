package in.projecteka.keycloak;

import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.*;
import java.util.stream.Stream;

@Slf4j
@SpringBootApplication
public class KeycloakSetupApplication  implements CommandLineRunner {

	@Value("${keycloak.server: http://keycloak:9001/auth}")
	private String keyCloakUrl;
	@Value("${keycloak.user: admin}")
	private String keyCloakAdminUsername;
	@Value("${keycloak.password: welcome}")
	private String keyCloakAdminPassword;

	public static void main(String[] args) {
		SpringApplication.run(KeycloakSetupApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Keycloak keycloak = KeycloakBuilder.builder()
				.serverUrl(keyCloakUrl)
				.realm("master")
				.username(keyCloakAdminUsername)
				.password(keyCloakAdminPassword)
				.clientId("admin-cli")
				.resteasyClient(
						new ResteasyClientBuilder()
								.connectionPoolSize(10).build()
				).build();

		createRealmAndAddClient(keycloak, "central-registry", "consent-manager", "http://localhost:9003");
		createRealmAndAddClient(keycloak, "consent-manager", "consent-manager", "http://localhost:9004");

		log.info("Setup Complete!");
	}

	private void createRealmAndAddClient(Keycloak keycloak, String realmName, String clientId, String redirectUrl) {
		List<RealmRepresentation> realmRepresentations = keycloak.realms().findAll();
		Optional<RealmRepresentation> realm = realmRepresentations.stream().filter(realmRepresentation -> realmRepresentation.getRealm().equals(realmName)).findAny();
		if (realm.isPresent()) {
			log.info("Realm with name [" + realmName + "] already exists.  Moving on..");
			return;
		}

		ClientRepresentation clientRepresentation = createClientRepresentation(clientId, redirectUrl);

		CredentialRepresentation credential = new CredentialRepresentation();
		credential.setTemporary(false);
		credential.setValue("welcome");
		credential.setType("password");

		UserRepresentation user = new UserRepresentation();
		user.setUsername("consent-service-admin-user");
		user.setEnabled(true);
		user.setEmailVerified(false);
		user.setCredentials(Arrays.asList(credential));
		user.setRealmRoles(Arrays.asList("offline_access", "uma_authorization"));
		Map<String, List<String>> clientRoles = new HashMap<>();
		clientRoles.put("realm-management", Arrays.asList("manage-users"));
		user.setClientRoles(clientRoles);

		RealmRepresentation realmRepresentation = new RealmRepresentation();
		realmRepresentation.setRealm(realmName);
		realmRepresentation.setEnabled(true);
		realmRepresentation.setClients(Arrays.asList(clientRepresentation));
		realmRepresentation.setUsers(Arrays.asList(user));

		keycloak.realms().create(realmRepresentation);
		log.info("Created Realm [" + realmName + "]");
	}

	private ClientRepresentation createClientRepresentation(String clientId, String redirectUrl) {
		ClientRepresentation clientRepresentation = new ClientRepresentation();
		clientRepresentation.setClientId(clientId);
		clientRepresentation.setRedirectUris(Arrays.asList(redirectUrl));
		clientRepresentation.setSurrogateAuthRequired(false);
		clientRepresentation.setEnabled(true);
		clientRepresentation.setAlwaysDisplayInConsole(false);
		clientRepresentation.setClientAuthenticatorType("client-secret");
		clientRepresentation.setNotBefore(0);
		clientRepresentation.setBearerOnly(false);
		clientRepresentation.setConsentRequired(false);
		clientRepresentation.setStandardFlowEnabled(true);
		clientRepresentation.setImplicitFlowEnabled(false);
		clientRepresentation.setDirectAccessGrantsEnabled(true);
		clientRepresentation.setServiceAccountsEnabled(true);
		clientRepresentation.setPublicClient(false);
		clientRepresentation.setFrontchannelLogout(false);
		clientRepresentation.setProtocol("openid-connect");
		clientRepresentation.setFullScopeAllowed(true);
		clientRepresentation.setAuthorizationServicesEnabled(true);
		return clientRepresentation;
	}

}