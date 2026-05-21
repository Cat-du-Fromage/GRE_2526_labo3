package ch.heig.gre.maze;

import ch.heig.gre.graph.GraphObserver;
import ch.heig.gre.graph.VertexLabelling;

import java.util.Arrays;

/**
 * <p>Implémentation de {@link VertexLabelling} pour des étiquettes de type int représentant des distances.</p>
 *
 * <p>Toutes les valeurs sont initialisées à {@link Integer#MAX_VALUE}, symbolisant une distance infinie.</p>
 */
public final class DistanceLabelling  implements VertexLabelling<Integer> {
  private final int[] labels;

  public DistanceLabelling(int size) {
    this.labels = new int[size];
    Arrays.fill(this.labels, Integer.MAX_VALUE);
  }

  @Override
  public Integer getLabel(int v) {
    return labels[v];
  }

  @Override
  public void setLabel(int v, Integer label) {
    labels[v] = label;
  }
}
