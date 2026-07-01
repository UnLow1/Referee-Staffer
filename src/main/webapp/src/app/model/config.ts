export interface Config {
  id: number;
  value: number;
  name: string;
  /** Backend-driven UI grouping ("potential" | "difficulty" | "effective"). */
  group?: string;
  /** Backend-driven one-line description, shown under the input. */
  description?: string;
}
