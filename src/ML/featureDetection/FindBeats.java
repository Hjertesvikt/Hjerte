/*
 * First, the algorithm applies a low-pass filter. 
 * Filtered audio data are divided into windows of samples.
 * A peak detection algorithm is applied to each window and identifies as peaks 
 * all values above a certain threshold. 

test netbeans 5

 * For each peak the distance in number of samples between it and its neighbouring peaks is stored.
 * For each distance between peaks the algorithm counts how many times it has been detected. 
 * Once all windows are processed the algorithm has built a map distanceHistogram where for each 
 * possible distance its number of occurrences is stored.
 */
package ML.featureDetection;

import java.util.ArrayList;
import java.util.BitSet;

/**
 *
 * @author jplr
 */
public class FindBeats {

    // the various beats frequencies
    static float[] auto_correlation;

    // the label for each beat in "auto_correlation"
    static float[] labels;

    // bag of beats probably including S1, S2, S3, S4 sounds
    private ArrayList moreBeatEvents = new ArrayList();

    // S1 beats as found in data flow, maybe wrong because of spikes
    private ArrayList probableS1Beats = new ArrayList();

    // Normalized data that is correlated to moreBeatEvents and probableS1Beats
    private float[] normalizedData;

   // average value of sound 
    float aver;

    // max value of sound
    float maxi ;
            
    /**
     */
    public FindBeats() {
    }
    
    private void averMax(float[] data) {
        int idx = 0, sum = 0;
        // find this file average
        while (idx < data.length) {
            if (data[idx] > 0) {
                // in order to find average value
                sum += data[idx];
                // in order to find maximum value
                if (data[idx] > maxi) {
                    maxi = data[idx];
                }
            }
            idx++;
        }

        // find this file average
        aver = sum / data.length;
    }

    /**
     * It calculates the beat in dataIn, this is not the same as heart beat rate
     * because the heart has several sounds inside one beat We will count
     * downward changes that occur at intensities between max value and average
     * value in order to stay away from noise A good reference is a0004.wav that
     * has 36 beats and lasts 36 seconds.
     *
     * @param dataIn
     * @param sampling_rate
     *
     */
    void calcBeat1(float[] data, int sampling_rate, int beatSec) {
        float[] dataIn = data;
        ArrayList beats = null;
        ArrayList candidateBeats = new ArrayList();
        float sum = 0, max = 0, ave;
        int idx;
        ArrayList preBeats = new ArrayList();
        ArrayList prepreBeats = new ArrayList();
        float s2Shift = (float) 0.15;
        float earlyS1, lateS1;

        // average beat rate may be one per second.
        int nbSecInFile = dataIn.length / sampling_rate;

        idx = 0;
        // find this file average
        while (idx < dataIn.length) {
            if (dataIn[idx] > 0) {
                // in order to find average value
                sum += dataIn[idx];
                // in order to find maximum value
                if (dataIn[idx] > max) {
                    max = dataIn[idx];
                }
            }
            idx++;
        }

        // find this file average
        ave = sum / dataIn.length;

        // evaluating the tresholdS1        
        int treshFind = 6;

        if (beatSec == 0) {
            // assume 60
            beatSec = 60;
        }

        int floor_low, floor_high, ceiling_low, ceiling_high;
        floor_low = (int) (beatSec * 0.8335);       // 25 beats per minute (25 S1 + 25 S2)
        floor_high = (int) (beatSec * 1.6667);      // 50 beats per minute 
        ceiling_low = (int) (beatSec * 2.6667);     // 80 beats per minute 
        ceiling_high = (int) (beatSec * 5.3334);   // 160 beats per minute 

        earlyS1 = sampling_rate * (float) 0.5;
        lateS1 = sampling_rate * (float) 1.5;

        while (treshFind > 0) {
            beats = calcBeatFind(dataIn, earlyS1, lateS1, s2Shift, treshFind);

            // Number of beats per minute
            int nbBeats = (beats.size() * 60) / nbSecInFile;

            if ((nbBeats > (floor_low)) && (nbBeats < (ceiling_high))) {
                if ((nbBeats > (floor_high)) && (nbBeats < (ceiling_low))) {

                    // Find next sounds in this beat
                    moreBeatEvents = calcBeatFind(dataIn, earlyS1, lateS1, s2Shift, treshFind);

                    normalizedData = dataIn;
                    //                samplingRate = sampling_rate;
                    probableS1Beats = beats;
                    return;
                }
                // Normally we should converge, if not then return last heart rate
                int un = preBeats.size() - prepreBeats.size();
                int deux = beats.size() - preBeats.size();
                if ((un < deux) && (un > 0) && (deux > 0) && (preBeats.size() > 0)) {
                    // Find next sounds in this beat
                    moreBeatEvents = calcBeatFind(dataIn, earlyS1, lateS1, s2Shift, treshFind);

                    normalizedData = dataIn;
                    //                samplingRate = sampling_rate;
                    probableS1Beats = preBeats;
                    return;
                } else {
                    prepreBeats = preBeats;
                    preBeats = beats;
                }
                if ((nbBeats < (floor_high)) && (candidateBeats.size() < beats.size())) {
                    candidateBeats = beats;
                } else if ((nbBeats > (ceiling_low)) && (candidateBeats.size() > beats.size())) {
                    candidateBeats = beats;
                }
            } else if (nbBeats > (nbSecInFile * 4)) {
                // Something wrong with dataIn, way too much beats, so we filter it heavily
                Resample resp = new Resample();
                dataIn = resp.downSample(dataIn, sampling_rate, 1024);
                continue;
            } else if (nbBeats < (nbSecInFile / 3)) {
                // Something wrong with dataIn, not enough beats, so we normalize dataIn
                NormalizeBeat nb = new NormalizeBeat();
                dataIn = nb.normalizeAmplitude(dataIn);
                continue;
            }
            // Finer grain as tresholdS1 becomes only slightly higher than average
            if (treshFind > 1) {
                treshFind -= 0.5;
            } else {
                treshFind -= 0.125;
            }
        }
        // Find next sounds in this beat
        moreBeatEvents = calcBeatFind(dataIn, earlyS1, lateS1, s2Shift, treshFind);

        normalizedData = dataIn;
//        samplingRate = sampling_rate;
        probableS1Beats = candidateBeats;
        return;
    }

    /**
     * It calculates the beat in dataIn, this is not the same as heart beat rate
     * because the heart has several sounds inside one beat We will count
     * downward changes that occur at intensities between max value and average
     * value in order to stay away from noise. A good reference is a0004.wav
     * that has 36 beats and lasts 35 seconds.
     *
     * this param makes it possible to adjust the best possible the window
     * around each heart beat. contrary to what it may seems, it is an average
     * rate, and we need to determine each beat time, so it is better to start
     * with a correct average rate than a "one size fits all" rate of one beat
     * per second.
     *
     * @param beatSec
     */
    public void calcBeat2(float[] dataIn, int sampling_rate, int beatSec) {
        ArrayList beats = null;
        ArrayList candidateBeats = new ArrayList();
        ArrayList preBeats = new ArrayList();
        ArrayList prepreBeats = new ArrayList();
        float s2Shift = (float) 0.15;
        float earlyS1, lateS1;

        // average beat rate may be one per second.
        int nbSecInFile = dataIn.length / sampling_rate;

        // evaluating the tresholdS1        
        int treshFind = 6;

        if (beatSec == 0) {
            // assume 60
            beatSec = 60;
        }
        int floor_low, floor_high, ceiling_low, ceiling_high;
        floor_low = (int) (beatSec * 0.8335);       // 25 beats per minute (25 S1 + 25 S2)
        floor_high = (int) (beatSec * 1.6667);      // 50 beats per minute 
        ceiling_low = (int) (beatSec * 2.6667);     // 80 beats per minute 
        ceiling_high = (int) (beatSec * 5.3334);   // 160 beats per minute 

        earlyS1 = sampling_rate * (float) 0.5;
        lateS1 = sampling_rate * (float) 1.5;

        NormalizeBeat nb = new NormalizeBeat();

        while (treshFind > 0) {
            beats = calcBeatFind(dataIn, earlyS1, lateS1, s2Shift, treshFind);

            // Number of beats per minute
            int nbBeats = (beats.size() * 60) / nbSecInFile;

            if ((nbBeats > (floor_low)) && (nbBeats < (ceiling_high))) {
                if ((nbBeats > (floor_high)) && (nbBeats < (ceiling_low))) {

                    // Find next sounds in this beat
                    moreBeatEvents = calcBeatFind(dataIn, earlyS1, lateS1, s2Shift,
                            (float) (treshFind / 2));

                    normalizedData = dataIn;
                    probableS1Beats = beats;
                    return;
                }
                // Normally we should converge, if not then return last heart rate
                int un = preBeats.size() - prepreBeats.size();
                int deux = beats.size() - preBeats.size();
                if ((un < deux) && (un > 0) && (deux > 0) && (preBeats.size() > 0)) {
                    // Find next sounds in this beat
                    moreBeatEvents = calcBeatFind(dataIn, earlyS1, lateS1, s2Shift, (float) (treshFind / 2));

                    normalizedData = dataIn;
                    probableS1Beats = preBeats;
                    return;
                } else {
                    prepreBeats = preBeats;
                    preBeats = beats;
                }
                if ((nbBeats < (floor_high)) && (candidateBeats.size() < beats.size())) {
                    candidateBeats = beats;
                } else if ((nbBeats > (ceiling_low)) && (candidateBeats.size() > beats.size())) {
                    candidateBeats = beats;
                }
            } else if (nbBeats > (nbSecInFile * 4)) {
                // Something wrong with dataIn, way too much beats, so we filter it heavily
                Resample resp = new Resample();
                dataIn = resp.downSample(dataIn, sampling_rate, 1024);
                continue;
            } else if (nbBeats < (nbSecInFile / 3)) {
                // Something wrong with dataIn, not enough beats, so we normalize dataIn
                dataIn = nb.normalizeAmplitude(dataIn);
                continue;
            }
            // Finer grain as tresholdS1 becomes only slightly higher than average
            treshFind *= 0.8;
            if (treshFind < 0.005) {
                return;
            }
        }
        // Find next sounds in this beat
        moreBeatEvents = calcBeatFind(dataIn, earlyS1, lateS1, s2Shift, (float) (treshFind / 2));

        normalizedData = dataIn;
        probableS1Beats = candidateBeats;
        return;
    }

    /**
     * This is for finding S1 events, it is independant of sampling rate and
     * contains no prior knowledge about beat rate
     *
     * @param dataIn
     * @param sampling_rate
     * @param treshFind
     * @param ave
     * @param max
     * @return
     */
    ArrayList calcBeatFind(
            float[] dataIn,
            float earlyS1, float lateS1,
            float s2Shift, float treshFind
) {
            float ave, max ;
            int idxGlobal;
        ArrayList BeatS1Rate = new ArrayList();
        boolean S1Flag = false;
        float tresholdS1;
        float diff = 0;
        float lastAverage;

        // Calculate average and maximum positive value in the sound sample
        // Results in fields aver and maxi
        averMax(dataIn) ;
        ave = aver; 
        max = maxi ;
        
        if(max > 1) {
            max = (float) 0.99 ;
        }
        
        if(ave <= 0) {
            ave = max / 3 ;
        }
        
        /**/
        tresholdS1 = (ave + (treshFind * max)) / (treshFind + 1);

        idxGlobal = 1;
        while (idxGlobal < dataIn.length) {
            lastAverage = lastWinAve(dataIn, idxGlobal);
            if ((dataIn[idxGlobal] > tresholdS1) && (S1Flag == true)) {
                if (lastAverage < tresholdS1) {
                    // We went through the tresholdS1 upward, while counting was forbidden
                    // We need to reautorize it at the next downward tresholdS1
                    S1Flag = false;
                }
            }
            if ((dataIn[idxGlobal] < tresholdS1) && (S1Flag == false) && (lastAverage > tresholdS1)) {
                // So we are in a situation when we went through the tresholdS1 when we went 
                // from [idxGlobal-1] to [idxGlobal]
                // Let's count a beat, and rise a flag to count only once downward
                int un = BeatS1Rate.size();
                if (un > 0) {
                    diff = idxGlobal - ((Integer) BeatS1Rate.get(un - 1)).intValue();
                } else {
                    diff = earlyS1 + 1;
                }

                // Is it possibly too early?
                if (diff < earlyS1) {
                    idxGlobal++;
                    continue;
                }
                // Is it possibly too late?
                if (diff > lateS1) {
                    tresholdS1 = (float) ((double) tresholdS1 * 0.85D);
                    if (un > 1) {
                        idxGlobal = ((Integer) BeatS1Rate.get(un - 2)).intValue();
                        continue;
                    }
                }
                BeatS1Rate.add(Integer.valueOf(idxGlobal));
                S1Flag = true;

                // try to move idxGlobal a bit, in a effort to thwart spikes
                idxGlobal += s2Shift;
            }
            idxGlobal++;
        }
        return BeatS1Rate;
    }

    public ArrayList getMoreBeats() {
        return moreBeatEvents;
    }

    public ArrayList getProbableBeats() {
        return probableS1Beats;
    }

    public float[] getNormalizedData() {
        return normalizedData;
    }

    private float lastWinAve(float[] dataIn, int idxGlbal) {
        int cnt = 0, idx;
        int idxGlobal = idxGlbal ;
        float prevWindowAve = 0;

        if (idxGlobal < 30) {
            idxGlobal = 30;
        }

        cnt = 0;
        prevWindowAve = 0;
        idx = idxGlobal - 30;
        while (idx < idxGlobal) {
            if(idx < dataIn.length) {
            if (dataIn[idx] > 0) {
                prevWindowAve += dataIn[idx];
                cnt++;
            }
            idx++;
            }
            else {
    //             int y = 0 ;
    //            break ;
            }
        }
        if (cnt > 0) {
            return prevWindowAve / cnt;
        } else {
            return dataIn[dataIn.length - 1];
        }
    }

    /**
     * The purpose of this method is to calculate a "signature" of the beat. It
     * is a bit string, long as a beat lasts, and having only "0" or "1" values
     * It is created by saturing the signal and compressing it with RLL.
     *
     * It is supposed to be faster than FFT
     *
     */
    public ArrayList beatSign(float[] data) {
        // the signature is found
        BitSet binary = new BitSet();

        int jdx = 0;
        float aver = 0, maxi = 0;
        float treshold = 0;
        // find this file average
        while (jdx < data.length) {
            aver += data[jdx];
            if (maxi < data[jdx]) {
                maxi = data[jdx];
            }
            jdx++;
        }
        aver = aver / data.length;
        treshold = ((2 * aver) + maxi) / 3;

        // treshold is a value between the average value and the max value
        // 
        int idx = 0;
        // find this file average
        while (idx < data.length) {
            if (data[idx] > treshold) {
                binary.set(idx);
            } else {
                binary.clear(idx);
            }
            idx++;
        }

        // Now do the Run Limited Length algorithm
        int j;
        ArrayList reslt = new ArrayList();

        // We start always with a count of "1" at index == "0"
        int k = 0;
        boolean a = binary.get(0);
        while (a != true) {
            a = binary.get(k);
            k++;
        }

        for (int i = 0; i < binary.length(); i++) {
            int runLength = 1;
            while (i + 1 < binary.length() && binary.get(i) == binary.get(i + 1)) {
                runLength++;
                i++;
            }

            reslt.add(new Integer(runLength));
        }
        return reslt;
    }

    void calcBeat(float[] data_norm, int heart_rate, int sampling_rate, int rough) {
        // Get a first pass that unfortunately will caught some glitches as valid heartbeats
        calcBeat2(data_norm, sampling_rate, heart_rate);

        // Find at least 80% of reasonable heart beats, at the price of missing some heart beats
        int nbHeartBeats = this.probableS1Beats.size();
        
        if(nbHeartBeats == 0) {
            return ;
        }
//        int duration = this.dataIn.length / this.sampling_rate ;
        int interval = data_norm.length / nbHeartBeats;

        ArrayList pipehole = new ArrayList(nbHeartBeats);

        int i = 0;
        while (i < nbHeartBeats) {
            Integer beatDuration = (Integer) this.probableS1Beats.get(i);
            if ((beatDuration.intValue() < (interval * 0.4))
                    || (beatDuration.intValue() > (interval * 1.5))) {
                Object next = this.probableS1Beats.get(i + 1);
                if (next != null) {
                    pipehole.add(next);
                    // so it will be i + 2
                    i += 2;
                }
            } else {
                pipehole.add(beatDuration);
                i++;
            }
        }

        // Fill pipehole with probableS1Beats events, but at a rough scale
        rough = sampling_rate / 10;
        /*
        int i = 0 ;
        while(i < nbHeartBeats) {
            Integer beatDuration = (Integer) this.probableS1Beats.get(i);
            Integer deux = new Integer(beatDuration.intValue() / rough) ;
            pipehole.add(i, deux) ; 
            i++ ;
         } 
         */
    }
}
