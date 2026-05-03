export const environment = {
  production: true,
  // Same-origin in prod (Spring Boot serves both API and Angular bundle).
  // Override here if frontend ever moves to a separate host.
  apiBaseUrl: ''
};
