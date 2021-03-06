// =================================================================================================
// Copyright 2012 Jared Winick
// -------------------------------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this work except in compliance with the License.
// You may obtain a copy of the License in the LICENSE file, or at:
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// =================================================================================================

package trendulo.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

public class TemporalNGramSource implements Runnable {

	public final static int DEFAULT_BUFFER_SIZE = 10000;
	
	private int bufferSize;
	private ArrayBlockingQueue<List<TemporalNGram>> queue;
	private TemporalStringSequenceSource temporalStringSequenceSource;
	private boolean shutdown = false;
	private int nStart;
	private int nEnd;
	
	private Logger log = Logger.getLogger( TemporalNGramSource.class );
	
	public TemporalNGramSource( TemporalStringSequenceSource temporalStringSequenceSource ) {
		this( temporalStringSequenceSource, DEFAULT_BUFFER_SIZE );
	}
	
	public TemporalNGramSource( TemporalStringSequenceSource temporalStringSequenceSource, int bufferSize ) {
		this.temporalStringSequenceSource = temporalStringSequenceSource;
		this.bufferSize = bufferSize;
		
		queue = new ArrayBlockingQueue<List<TemporalNGram>>( bufferSize );
	}
	
	/**
	 * Returns a List of TemporalNGrams for a giving TemporalStringSequence
	 * @return
	 * @throws InterruptedException
	 */
	public List<TemporalNGram> take( ) throws InterruptedException {
		return queue.take();
	}

	public void run() {
	
		TemporalStringSequence temporalStringSequence = null;
		
		// loop until there is are no more strings or we have been asked to shutdown
		while ( ( temporalStringSequence = temporalStringSequenceSource.nextStringSequence() ) != null && shutdown == false ) {
			String cleanedStringSequence = StringUtilities.cleanStringSequence( temporalStringSequence.getStringSequence() );
			log.trace( String.format( "[%d] [%s]", temporalStringSequence.getTimestamp(), cleanedStringSequence ));
			
			// Generate the list of n-grams from the string sequence
			List<String> nGrams = NGramGenerator.generateAllNGramsInRange( cleanedStringSequence, nStart, nEnd );
			
			// For each n-gram, build a TemporalNGram and build a List which we will put on the queue
			List<TemporalNGram> temporalNGrams = new ArrayList<TemporalNGram>( nGrams.size() );
			for ( String nGram : nGrams ) {
				log.trace( nGram );
				temporalNGrams.add( new TemporalNGram( nGram, temporalStringSequence.getTimestamp() ) );
			}
			try {
				queue.put( temporalNGrams );
			} catch (InterruptedException e) {
				log.error( "Error while waiting for free space to become available on queue", e );
			}		
		}
	}

	/**
	 * Request the thread to shutdown by exiting the while loop of the run() method
	 */
	public void shutdown() {
		this.shutdown = true;
	}

	public int getnStart() {
		return nStart;
	}

	public void setnStart(int nStart) {
		this.nStart = nStart;
	}

	public int getnEnd() {
		return nEnd;
	}

	public void setnEnd(int nEnd) {
		this.nEnd = nEnd;
	}
}
