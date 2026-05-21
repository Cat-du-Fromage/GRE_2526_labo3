package ch.heig.gre.util;

import java.util.random.RandomGenerator;

/**
 * Classe utilitaire contenant des méthodes sur les tableaux.
 */
public final class ArrayUtil {
  private ArrayUtil() {}

  private static final RandomGenerator RNG = RandomGenerator.getDefault();

  /**
   * Mélange le tableau donné en place en utilisant l'algorithme de Fisher-Yates et le générateur de nombres aléatoires
   * par défaut.
   * @param array le tableau à mélanger
   * @return le tableau donné, mélangé en place
   * @throws NullPointerException si le tableau donné est null
   */
  public static int[] shuffle(int[] array) {
    return shuffle(array, RNG);
  }

  /**
   * Mélange le tableau donné en place en utilisant l'algorithme de Fisher-Yates et le générateur de nombres aléatoires
   * fourni en paramètre.
   * @param array le tableau à mélanger
   * @param rng le générateur de nombres aléatoires à utiliser pour le mélange
   * @return le tableau donné, mélangé en place
   * @throws NullPointerException si le tableau donné ou le générateur de nombres aléatoires sont null
   */
  public static int[] shuffle(int[] array, RandomGenerator rng) {
    for (int i = array.length - 1; i > 0; i--) {
      int j = rng.nextInt(0, i + 1);
      int tmp = array[i];
      array[i] = array[j];
      array[j] = tmp;
    }
    return array;
  }
}
