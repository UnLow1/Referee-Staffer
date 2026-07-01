// Wire shape mirroring com.jamex.refereestaffer.model.dto.DifficultyBreakdownDto.

export interface DifficultyBreakdown {
  matchId: number;
  total: number;
  parts: DifficultyParts;
  flags: DifficultyFlags;
}

export interface DifficultyParts {
  base: number;
  sameCity: number;
  top: number;
  bottom: number;
}

export interface DifficultyFlags {
  sameCity: boolean;
  isTop: boolean;
  isBot: boolean;
  pointsDiff: number;
}
