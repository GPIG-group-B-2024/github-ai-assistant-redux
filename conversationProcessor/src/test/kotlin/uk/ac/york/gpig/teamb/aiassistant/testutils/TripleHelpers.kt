package uk.ac.york.gpig.teamb.aiassistant.testutils

/** Convenience function to add a 3rd item to a pair and make it a Triple */
infix fun <A, B, C> Pair<A, B>.toTriple(third: C): Triple<A, B, C> = Triple(first, second, third)
