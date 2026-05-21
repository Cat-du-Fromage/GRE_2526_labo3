package ch.heig.gre.maze;

/**
 * Générateur de labyrinthes.
 *
 * @see MazeBuilder
 */
public interface MazeGenerator {
  /**
   * <p>Génère un nouveau labyrinthe en ajoutant ou supprimant des murs à une instance de {@link MazeBuilder} donnée.</p>
   *
   * <p>Lors de l'état initial, tous les murs possibles sont déjà placés.</p>
   *
   * @param builder Un {@link MazeBuilder} à qui déléguer les modifications de la structure de données.
   * @param from Sommet de départ, si l'algorithme utilisé en nécessite un.
   * @throws NullPointerException si {@code builder} est {@code null}.
   * @throws IllegalArgumentException si {@code from} n'est pas un sommet du graphe sous-jacent de {@code builder}.
   */
  void generate(MazeBuilder builder, int from);
}
