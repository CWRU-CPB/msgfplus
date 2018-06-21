package edu.ucsd.msjava.msdbsearch;

import edu.ucsd.msjava.misc.ProgressData;
import edu.ucsd.msjava.misc.ProgressReporter;

import java.io.PrintStream;
import java.util.List;
import org.apache.commons.io.output.NullOutputStream;

public class ConcurrentMSGFPlus {
    public static class RunMSGFPlus implements Runnable, ProgressReporter {
        private final ScoredSpectraMap specScanner;
        private final DBScanner scanner;
        SearchParams params;
        List<MSGFPlusMatch> resultList;
        private final int taskNum;
        
        /* +++Start CWRU-CPB 
         * Store the allow prefix matches search parameter value in object
         * property for passing to DBScanner
         **/
        private final boolean allowPrefixMatches;
        /* +++End CWRU-CPB */
        
        private ProgressData progress;

        @Override
        public void setProgressData(ProgressData data) {
            progress = data;
        }

        @Override
        public ProgressData getProgressData() {
            return progress;
        }

        public RunMSGFPlus(
                ScoredSpectraMap specScanner, CompactSuffixArray sa, SearchParams params, List<MSGFPlusMatch> resultList, int taskNum) {
            this.specScanner = specScanner;
            this.params = params;
            this.scanner = new DBScanner(
                    specScanner,
                    sa,
                    params.getEnzyme(),
                    params.getAASet(),
                    params.getNumMatchesPerSpec(),
                    params.getMinPeptideLength(),
                    params.getMaxPeptideLength(),
                    params.getMaxNumVariatsPerPeptide(),
                    params.getMinDeNovoScore(),
                    params.ignoreMetCleavage()
            );
            this.resultList = resultList;
            this.taskNum = taskNum;
            
            /* +++Start CWRU-CPB 
             * Extract value of allow prefix matches search parameter.
             */
            this.allowPrefixMatches = params.getPrefixMatchesAllowed();
            /* +++End CWRU-CPB */
            
            progress = null;
        }

        @Override
        public void run() {
            if (progress == null) {
                progress = new ProgressData();
            }
            
            PrintStream output;
            if (params.getVerbose()) {
                output = System.out;
            } else {
                output = new PrintStream(new NullOutputStream());
            }
            
            progress.stepRange(5.0);
            String threadName = Thread.currentThread().getName();
            output.println(threadName + ": Starting task " + taskNum);

            specScanner.setProgressObj(new ProgressData(progress));

            // Pre-process spectra
            long time = System.currentTimeMillis();
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            if (specScanner.getPepMassSpecKeyMap().size() == 0)
                specScanner.makePepMassSpecKeyMap();
            output.println(threadName + ": Preprocessing spectra...");
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            specScanner.preProcessSpectra();
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            output.print(threadName + ": Preprocessing spectra finished ");
            output.format("(elapsed time: %.2f sec)\n", (float) ((System.currentTimeMillis() - time) / 1000));

            specScanner.getProgressObj().setParentProgressObj(null);
            progress.report(5.0);
            progress.stepRange(80.0);
            scanner.setProgressObj(new ProgressData(progress));

            time = System.currentTimeMillis();
            // DB search
            output.println(threadName + ": Database search...");
            scanner.setThreadName(threadName);
            scanner.setPrintStream(output);

            int ntt = params.getNumTolerableTermini();
            if (params.getEnzyme() == null)
                ntt = 0;
            int nnet = 2 - ntt;
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            /* +++Start CWRU-CPB call DBScanner with new parameter controlling
             * prefix matching */
            scanner.dbSearch(nnet, allowPrefixMatches);
            /* +++End CWRU-CPB */
            
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            output.print(threadName + ": Database search finished ");
            output.format("(elapsed time: %.2f sec)\n", (float) ((System.currentTimeMillis() - time) / 1000));

            progress.stepRange(95.0);

            time = System.currentTimeMillis();
            output.println(threadName + ": Computing spectral E-values...");
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            scanner.computeSpecEValue(false);
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            output.print(threadName + ": Computing spectral E-values finished ");
            output.format("(elapsed time: %.2f sec)\n", (float) ((System.currentTimeMillis() - time) / 1000));

            scanner.getProgressObj().setParentProgressObj(null);
            progress.stepRange(100);
            
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            scanner.generateSpecIndexDBMatchMap();

            progress.report(30.0);

            if (params.outputAdditionalFeatures())
                scanner.addAdditionalFeatures();

            progress.report(60.0);

            scanner.addResultsToList(resultList);

            progress.report(100.0);
//			gen.addSpectrumIdentificationResults(scanner.getSpecIndexDBMatchMap());
            output.println(threadName + ": Task " + taskNum + " completed.");
        }
    }
}
