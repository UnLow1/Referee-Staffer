// Static personal info shown in the footer. Lives outside AppComponent so the root
// component isn't carrying personal data as fields, and so a future fork can swap it
// without touching component logic.
export const AUTHOR = {
  facebookUrl: 'https://www.facebook.com/UnLow1/',
  linkedInUrl: 'https://www.linkedin.com/in/adam-jamka-273289145/',
  email: 'mailto: adam.jamka.1995@gmail.com',
  steamUrl: 'https://steamcommunity.com/id/UnLow/'
} as const;
