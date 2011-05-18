package msscorer;

import msgf.NominalMass;

// this does not use edge scores
public class FastScorer implements SimpleDBSearchScorer<NominalMass> {

	protected float[] prefixScore = null;
	protected float[] suffixScore = null;
	private boolean mainIonDirection;
	
	public FastScorer(NewScoredSpectrum<NominalMass> scoredSpec, int peptideMass)
	{
		prefixScore = new float[peptideMass];
		suffixScore = new float[peptideMass];
		for(int i=0; i<prefixScore.length; i++)
			prefixScore[i] = Float.MIN_VALUE;
		for(int nominalMass=1; nominalMass<peptideMass; nominalMass++)
		{
			NominalMass node = new NominalMass(nominalMass);
			prefixScore[nominalMass] = scoredSpec.getNodeScore(node, true);
			suffixScore[nominalMass] = scoredSpec.getNodeScore(node, false);
		}
		mainIonDirection = scoredSpec.getMainIonDirection();
	}
	
	// fromIndex: inclusive, toIndex: exclusive
	@Override
	public int getScore(double[] prefixMassArr, int[] nominalPrefixMassArr, int fromIndex, int toIndex)
	{
		int score = 0;
		int peptideMass = nominalPrefixMassArr[toIndex-1];
		for(int i=fromIndex; i<toIndex-1; i++)
		{
			int prefixMass = nominalPrefixMassArr[i];
			int suffixMass = peptideMass - prefixMass;
			int curScore = Math.round(prefixScore[prefixMass]+suffixScore[suffixMass]);
			score += curScore;
		}
		return score;
	}

	@Override
	public int getNodeScore(NominalMass prefixMass, NominalMass suffixMass) {
		return Math.round(prefixScore[prefixMass.getNominalMass()]+suffixScore[suffixMass.getNominalMass()]);
	}

	@Override
	public int getEdgeScore(NominalMass curNode, NominalMass prevNode, float theoMass) {
		return 0;
	}
	
	@Override
	public boolean getMainIonDirection() {
		return mainIonDirection;
	}
}