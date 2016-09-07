/*
 * Copyright 2015 Mikhail Shugay
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.antigenomics.vdjdb.scoring;

import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.alignment.LinearGapAlignmentScoring;
import com.milaboratory.core.mutations.Mutations;
import com.milaboratory.core.sequence.Sequence;

import static com.milaboratory.core.mutations.Mutation.*;
import static com.milaboratory.core.mutations.Mutation.getPosition;

public class VdjdbAlignmentScoring implements AlignmentScoring {
    private final LinearGapAlignmentScoring scoring;
    private final float[] positionWeights;
    private float scoreThreshold;

    public VdjdbAlignmentScoring(LinearGapAlignmentScoring scoring,
                                 float[] positionWeights,
                                 float scoreThreshold) {
        this.scoring = scoring;
        this.positionWeights = positionWeights;
        this.scoreThreshold = scoreThreshold;
    }

    private float getPositionWeight(int pos, float factor) {
        int bin = (int) (pos * factor);
        return positionWeights[bin];
    }

    @Override
    public float computeBaseScore(Sequence reference) {
        float score = 0;
        float factor = (positionWeights.length - 1) / (float) (reference.size() - 1);

        for (int i = 0; i < reference.size(); ++i) {
            byte aa = reference.codeAt(i);
            score += scoring.getScore(aa, aa) * getPositionWeight(i, factor);
        }
        return score;
    }

    public float computeScore(Alignment alignment) {
        Sequence reference = alignment.getSequence1();
        return computeScore(alignment.getAbsoluteMutations(), computeBaseScore(reference), reference.size());
    }

    @Override
    public float computeScore(Mutations mutations, float baseScore, int refLength) {
        float score = baseScore;
        float factor = (positionWeights.length - 1) / (float) (refLength - 1);

        for (int i = 0; i < mutations.size(); ++i) {
            int mutation = mutations.getMutation(i);

            double deltaScore = 0;
            if (isInsertion(mutation)) {
                deltaScore += scoring.getGapPenalty();
            } else {
                byte from = getFrom(mutation);
                deltaScore += isDeletion(mutation) ? scoring.getGapPenalty() :
                        (scoring.getScore(from, getTo(mutation)));
                deltaScore -= scoring.getScore(from, from);
            }

            score += deltaScore * getPositionWeight(getPosition(mutation), factor);
        }

        return score;
    }

    public LinearGapAlignmentScoring getScoring() {
        return scoring;
    }

    public float[] getPositionWeights() {
        return positionWeights;
    }

    @Override
    public float getScoreThreshold() {
        return scoreThreshold;
    }

    @Override
    public AlignmentScoring withScoreThreshold(float scoreThreshold) {
        return new VdjdbAlignmentScoring(scoring,
                positionWeights, scoreThreshold);
    }
}
