package ch.heig.gre.groupO;

import ch.heig.gre.maze.MazeBuilder;
import ch.heig.gre.maze.MazeGenerator;
import ch.heig.gre.maze.Progression;
import ch.heig.gre.util.ArrayUtil;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

// TODO : reprendre le code du labo 1
public final class DfsGenerator implements MazeGenerator {
  @Override
  public void generate(MazeBuilder builder, int from) {
    int n = builder.topology().nbVertices();
    Progression[] progressions = new Progression[n];
    Arrays.fill(progressions, Progression.PENDING);
    dfs(builder, progressions, from);
  }

  /**
   * Exécute l'algorithme DFS (Depth-First Search) itératif pour générer un labyrinthe.
   *
   * 1. Initialisation du sommet de départ, marqué comme PROCESSING et empilé.
   * 2. Exploration en profondeur à partir du sommet courant (sommet au sommet de la pile).
   * 3. Pour chaque sommet traité :
   *    - Si un voisin PENDING existe, casse le mur vers ce voisin et l'empile.
   *    - Sinon, tous les voisins ont été visités : dépile et marque le sommet comme PROCESSED.
   * 4. Les voisins sont mélangés aléatoirement à la première visite pour garantir un labyrinthe aléatoire.
   * 5. Arrêt lorsque la pile est vide (tous les sommets accessibles ont été visités).
   *
   * @param builder      le constructeur du labyrinthe (gère les murs et les progressions)
   * @param progressions tableau d'état pour chaque sommet du graphe
   * @param from         le sommet de départ
   */
  private void dfs(MazeBuilder builder, Progression[] progressions, int from) {
    // Utilisation de la deque évite overhead inutile
    // Stack étend Vector, donc hérite de toutes ses méthode, moins efficace sur du single thread
    Deque<Integer> stack = new ArrayDeque<>();

    progressions[from] = Progression.PROCESSING;
    builder.progressions().setLabel(from, Progression.PROCESSING);
    stack.push(from);

    int nbVertices = builder.topology().nbVertices();
    int[][] neighbors = new int[nbVertices][];
    // index courant par sommet
    // permet de reprendre l'exploration d'un sommet à l'endroit où on s'était arrêté,
    // sans devoir réexplorer les voisins déjà visités
    int[] neighborIndex = new int[nbVertices];

    while (!stack.isEmpty()) {
      int current = stack.peek();//on regarde en haut de la pile (sans retirer)

      //on ne veut shuffle que une fois les voisins
      if(neighbors[current] == null)
        neighbors[current] = ArrayUtil.shuffle(builder.topology().neighbors(current));

      // on avance l'index juqu'au prochain voisin PENDING ou jusqu'à la fin des voisins
      int neighbor = -1;

      while(neighborIndex[current] < neighbors[current].length) {
        int candidate = neighbors[current][neighborIndex[current]];
        // avancer même si PENDING, on évite de retourner en arrière
        neighborIndex[current]++;
        if (progressions[candidate] == Progression.PENDING) {
          neighbor = candidate;
          break;
        }
      }

      if (neighbor == -1) {// Tous les voisins de current ont été visité => finalise
        stack.pop();//ici on retire de la pile!
        progressions[current] = Progression.PROCESSED;
        builder.progressions().setLabel(current, Progression.PROCESSED);
      }
      else {// casse le mur vers le voisin + explore
        builder.removeWall(current, neighbor);
        progressions[neighbor] = Progression.PROCESSING;
        builder.progressions().setLabel(neighbor, Progression.PROCESSING);
        stack.push(neighbor);//on ajoute le voisin en haut de la pile(sans retirer le précédent)
      }
    }
  }
}
