package ch.heig.gre.maze;

import java.util.*;

/**
 * <p>Un stockage de métadonnées génériques, permettant d'associer des paires clé-valeur à un objet</p>.
 *
 * <p>Chaque clé est un objet de type {@link Metadata.Key}, qui contient un nom et un type de valeur. L'ordre d'insertion
 * des paires clé-valeur est préservé.<p>
 *
 * <p>La gestion des doublons de noms de clés n'est pas définie, et peut entraîner des comportements indéterminés.</p>
 */
public final class Metadata implements Iterable<Map.Entry<Metadata.Key<?>, Object>> {

  /**
   * <p>Clé de métadonnée générique, associant un nom et un type de valeur.</p>
   * @param name Nom de la clé.
   * @param type Type de la valeur associée à la clé.
   * @param <T> Type de la valeur associée à la clé.
   */
  public record Key<T>(String name, Class<T> type) {}

  // ArrayList pour préserver l'ordre d'insertion.
  private final ArrayList<Map.Entry<Metadata.Key<?>, Object>> entries = new ArrayList<>();

  /**
   * <p>Ajoute une paire clé-valeur aux métdadonnées.</p>
   * @param key Clé de la métadonnée.
   * @param value Valeur associée à la clé. Doit être du type spécifié par la clé.
   * @param <T> Type de la valeur associée à la clé.
   */
  public <T> void put(Key<T> key, T value) {
    entries.add(Map.entry(key, value));
  }

  /**
   * <p>Récupère la valeur associée à une clé donnée.</p>
   *
   * @param key Clé de la métadonnée à récupérer.
   * @param <T> Type de la valeur associée à la clé.
   * @return La valeur associée à la clé.
   * @throws IllegalArgumentException si la clé n'est pas présente dans les métadonnées.
   * @throws ClassCastException si la valeur associée à la clé n'est pas du type spécifié par la clé.
   */
  @SuppressWarnings("unchecked")
  public <T> T get(Key<T> key) {
    for (Map.Entry<Key<?>, Object> entry : entries) {
      if (entry.getKey().equals(key)) {
        if (!key.type().isInstance(entry.getValue())) {
          throw new ClassCastException("Value for key " + key + " is not of type " + key.type().getName());
        }
        return (T) entry.getValue();
      }
    }

    throw new IllegalArgumentException("Key " + key + " not found in metadata");
  }

  @Override
  public Iterator<Map.Entry<Key<?>, Object>> iterator() {
    return entries.iterator();
  }
}
