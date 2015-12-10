package pdp.access;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;
import pdp.domain.EntityMetaData;
import pdp.serviceregistry.ServiceRegistry;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

public class PolicyIdpAccessEnforcerFilter extends BasicAuthenticationFilter {

  private final ServiceRegistry serviceRegsitry;

  public PolicyIdpAccessEnforcerFilter(AuthenticationManager authenticationManager, ServiceRegistry serviceRegsitry) {
    super(authenticationManager);
    this.serviceRegsitry = serviceRegsitry;
  }

  @Override
  protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException {
    //check headers for enrichment of the Authentication
    String idpEntityId = request.getHeader("X-IDP-ENTITY-ID");
    String nameId = request.getHeader("X-UNSPECIFIED-NAME-ID");
    String displayName = request.getHeader("X-DISPLAY-NAME");

    if (!StringUtils.hasText(idpEntityId) || !StringUtils.hasText(nameId) || !StringUtils.hasText(displayName)) {
      //any policy idp access checks will fail, but it might be that this call is not for something that requires access
      return;
    }

    Set<EntityMetaData> idpEntities = serviceRegsitry.identityProvidersByAuthenticatingAuthority(idpEntityId);

    //By contract we have at least one Idp - otherwise an Exception is already raised
    String institutionId = idpEntities.iterator().next().getInstitutionId();
    Set<EntityMetaData> spEntities = serviceRegsitry.serviceProvidersByInstitutionId(institutionId);

    RunAsFederatedUser policyIdpAccessAwarePrincipal = new RunAsFederatedUser(nameId, idpEntityId, displayName, idpEntities, spEntities, authResult.getAuthorities());

    SecurityContextHolder.getContext().setAuthentication(new PolicyIdpAccessAwareToken(policyIdpAccessAwarePrincipal));

  }

}