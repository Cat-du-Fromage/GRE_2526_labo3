package ch.heig.gre.gui.impl;

import ch.heig.gre.graph.GraphObserver;
import ch.heig.gre.graph.VertexLabelling;
import ch.heig.gre.maze.MazeSolver;

import java.util.Arrays;

public final class SolverMonitor implements VertexLabelling<Integer> {
  private final int[] labels;
  private final GraphObserver observer;

  public SolverMonitor(int size, GraphObserver observer) {
    this.labels = new int[size];
    Arrays.fill(this.labels, Integer.MAX_VALUE);
    this.observer = observer;
  }

  @Override
  public Integer getLabel(int v) {
    return labels[v];
  }

  @Override
  public void setLabel(int v, Integer label) {
    if (label == labels[v]) return;

    labels[v] = label;
    observer.onVertexChanged(v);
  }
}
