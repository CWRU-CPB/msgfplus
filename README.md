Phospho-MSGF+ is a modification to MS-GF+ that optimizes searches of peptide databases (not full protein databases) that have been recoded to contain B/U/Z corresponding to phosphorylated S/T/Y amino acids. The key changes are:

  1. Addition of B/U/Z to the standard amino acid set
  2. Addition of a "-preDigestEnzyme" input parameter, that specifies what enzyme was used to create the peptide sequences in the input database. The peptides may include missed cleavages, so the normal enzyme flag should be set to "no-digest" and this flag is used to select the correct scoring parameters for the experimental setup.
  3. Addition of a "-allowPrefixMathches" input parameter that controls use of the suffix array. During a peptide search with missed cleavages, use of the suffix array can result in a high number of redundant identifications, and disabling use of the suffix array works around this at a small performance penalty. 
