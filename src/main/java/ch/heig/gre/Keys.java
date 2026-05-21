package ch.heig.gre;

import ch.heig.gre.maze.Metadata;

/**
 * Contient les clés des métadonnées nécessaire à ce laboratoire.
 */
public final class Keys {
  private Keys() {}

  /**
   * Clé de la métadonnée contenant la longueur (somme des poids des arcs) du plus court chemin trouvé par
   * l'algorithme de résolution.
   */
  public static final Metadata.Key<Integer> LENGTH = new Metadata.Key<>("Longueur", Integer.class);

  /** Clé de la métadonnée contenant le nombre de sommets traités par l'algorithme. */
  public static final Metadata.Key<Integer> NB_PROCESSED_VERTICES = new Metadata.Key<>("Nb sommets traités", Integer.class);
}
