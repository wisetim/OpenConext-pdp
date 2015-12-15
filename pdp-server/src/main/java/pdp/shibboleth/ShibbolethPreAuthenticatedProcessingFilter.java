package pdp.shibboleth;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StringUtils;
import pdp.access.FederatedUser;
import pdp.access.FederatedUserBuilder;
import pdp.access.PolicyIdpAccessAwareToken;
import pdp.access.RunAsFederatedUser;
import pdp.serviceregistry.ServiceRegistry;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;
import static pdp.access.FederatedUserBuilder.apiAuthorities;

public class ShibbolethPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

  private final FederatedUserBuilder federatedUserBuilder;

  public ShibbolethPreAuthenticatedProcessingFilter(AuthenticationManager authenticationManager, ServiceRegistry serviceRegistry) {
    super();
    setAuthenticationManager(authenticationManager);
    this.federatedUserBuilder = new FederatedUserBuilder(serviceRegistry);
    setCheckForPrincipalChanges(true);
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
    Optional<FederatedUser> federatedUserOptional = hasText(request.getHeader(FederatedUserBuilder.X_IMPERSONATE)) ?
            federatedUserBuilder.basicAuthUser(request, new UsernamePasswordAuthenticationToken("N/A", "N/A", apiAuthorities)) :
            federatedUserBuilder.shibUser(request);
    //null is how the contract for AbstractPreAuthenticatedProcessingFilter works
    return federatedUserOptional.isPresent() ? federatedUserOptional.get() : null;
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return "N/A";
  }

  @Override
  protected boolean principalChanged(HttpServletRequest request, Authentication currentAuthentication) {
    //the Javascript client has the functionality to impersonate an user. If this functionality if off then
    //only need to check if the currentAuthentication is not the cached impersonation
    return hasText(request.getHeader(FederatedUserBuilder.X_IMPERSONATE)) || currentAuthentication.getPrincipal() instanceof RunAsFederatedUser;
  }
}
