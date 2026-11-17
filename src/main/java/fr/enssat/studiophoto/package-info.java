/**
 * Application de gestion d'un studio de photographie.
 *
 * <p>Organisation en couches :</p>
 * <ul>
 *   <li>{@code resource} : ressources REST JAX-RS (entrée HTTP, codes de retour)</li>
 *   <li>{@code service} : règles métier, appelées par les ressources</li>
 *   <li>{@code dao} : accès à la base MySQL en JDBC (une interface + une implémentation par table)</li>
 *   <li>{@code model} : objets métier, sérialisés en JSON par JSON-B</li>
 *   <li>{@code dto} : objets d'échange de l'API qui ne sont pas des objets métier</li>
 *   <li>{@code exception} : exceptions métier, converties en réponses HTTP</li>
 * </ul>
 */
package fr.enssat.studiophoto;
