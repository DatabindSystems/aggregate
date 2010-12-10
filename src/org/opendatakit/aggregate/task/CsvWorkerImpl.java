/*
 * Copyright (C) 2010 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.aggregate.task;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Date;

import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.form.Form;
import org.opendatakit.aggregate.form.PersistentResults;
import org.opendatakit.aggregate.form.PersistentResults.Status;
import org.opendatakit.aggregate.format.SubmissionFormatter;
import org.opendatakit.aggregate.format.table.CsvFormatter;
import org.opendatakit.aggregate.query.submission.QueryByDate;
import org.opendatakit.aggregate.submission.Submission;
import org.opendatakit.aggregate.submission.SubmissionKey;
import org.opendatakit.common.constants.BasicConsts;
import org.opendatakit.common.constants.HtmlConsts;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.security.User;

/**
 * Common worker implementation for the generation of csv files.
 * 
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 * 
 */
public class CsvWorkerImpl {

	private final Form form;
	private final SubmissionKey persistentResultsKey;
	private final Long attemptCount;
	private final String baseWebServerUrl;
	private final Datastore datastore;
	private final User user;

	public CsvWorkerImpl( Form form, SubmissionKey persistentResultsKey, Long attemptCount, String baseWebServerUrl, Datastore datastore, User user) {
		this.form = form;
		this.persistentResultsKey = persistentResultsKey;
		this.attemptCount = attemptCount;
		this.baseWebServerUrl = baseWebServerUrl;
		this.datastore = datastore;
		this.user = user;
	}

	public void generateCsv() {
		try {
		    ByteArrayOutputStream stream = new ByteArrayOutputStream();
		    PrintWriter pw = new PrintWriter(stream);
		   
		    // create CSV
		    QueryByDate query = new QueryByDate(form, BasicConsts.EPOCH, false, ServletConsts.FETCH_LIMIT,
		    		datastore, user);
		    SubmissionFormatter formatter = new CsvFormatter(form, baseWebServerUrl, pw, null);
		    formatter.processSubmissions(query.getResultSubmissions());
	
		    // output file
		    pw.close();
		    byte[] outputFile = stream.toByteArray();
	
		    Submission s = Submission.fetchSubmission(persistentResultsKey.splitSubmissionKey(), datastore, user);
		    PersistentResults r = new PersistentResults(s);
		    if ( r.getAttemptCount() == attemptCount ) {
				r.setResultFile(outputFile, HtmlConsts.RESP_TYPE_PLAIN, 
						Long.valueOf(outputFile.length), 
						form.getViewableFormNameSuitableAsFileName() + ServletConsts.CSV_FILENAME_APPEND, datastore, user);
				r.setStatus(Status.AVAILABLE);
				r.setCompletionDate(new Date());
		    }
			r.persist(datastore, user);
		} catch ( Exception e ) {
			failureRecovery(e);
		}
	  }
	  
	private void failureRecovery(Exception e) {
		// four possible exceptions:
		// ODKFormNotFoundException, ODKDatastoreException, ODKIncompleteSubmissionData, Exception
		e.printStackTrace();
	    Submission s;
		try {
			s = Submission.fetchSubmission(persistentResultsKey.splitSubmissionKey(), datastore, user);
		    PersistentResults r = new PersistentResults(s);
		    if ( attemptCount == r.getAttemptCount() ) {
		    	r.deleteResultFile(datastore, user);
		    	r.setStatus(Status.FAILED);
		    	r.persist(datastore, user);
		    }
		} catch (Exception ex) {
			// something is hosed -- don't attempt to continue.
			// watchdog: find this once lastRetryDate is late
		}
	  }
}