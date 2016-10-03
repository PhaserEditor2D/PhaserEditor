/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.index;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.jsdt.internal.core.util.Messages;

/**
 * <p>Uses a single {@link Job} to run any number of {@link ISafeRunnable}
 * scheduled to run at a later time.  This is to prevent a different {@link Job} from
 * having to be created for every single {@link ISafeRunnable} to be run after a delayed time.</p>
 * 
 * <p><b>NOTE:</b> All times are in milliseconds.</p>
 */
class PostponedRunnablesManager {
	
	/**
	 * <p>Lock to use whenever {@link #fRunnablesProcessingJob} is being accessed.</p>
	 */
	private static final Object fRunnablesProcessingJobLock = new Object();
	
	/**
	 * <p>The single {@link Job} that handles running the runnables.</p>
	 */
	private static RunnablesProcessingJob fRunnablesProcessingJob;
	
	/**
	 * <p>Use to add an {@link ISafeRunnable} to be run at a future time.
	 * The given delay is a minimum time to wait before running the runnable,
	 * it could be longer before the runnable is run, this is dependent on {@link Job}
	 * scheduling, the number of runnables to be run, and the running time of each
	 * runnable since all runnables are run on the same {@link Job}</p>
	 * 
	 * @param runnable {@link ISafeRunnable} to be run after the given delay
	 * @param delay time in milliseconds to wait before running the given runnable.
	 * 
	 * @return the created postponed runnable.  Pass this to {@link #cancelPostponedRunnable(Object)}
	 * in order to cancel the postponed runnable before it runs.
	 * 
	 * @see #cancelPostponedRunnable(ISafeRunnable)
	 */
	protected static Object addPostponedRunnable(ISafeRunnable runnable, int delay) {
		Object result;
		synchronized (fRunnablesProcessingJobLock) {
			if(fRunnablesProcessingJob == null) {
				fRunnablesProcessingJob = new RunnablesProcessingJob();
			}
			
			result = fRunnablesProcessingJob.addPostponedRunnable(runnable, delay);
		}
		
		return result;
	}
	
	/**
	 * <p>Use to cancel a postponed runnable added with {@link #addPostponedRunnable(ISafeRunnable, int)}.</p>
	 * 
	 * @param runnable the resulting runnable from a call to {@link #addPostponedRunnable(ISafeRunnable, int)} to cancel
	 * 
	 * @throws InvalidParameterException if the given runnable was not the result of a
	 * call to {@link #addPostponedRunnable(ISafeRunnable, int)}
	 * 
	 * @see #addPostponedRunnable(ISafeRunnable, int)
	 */
	protected static void cancelPostponedRunnable(Object runnable) throws IllegalArgumentException {
		synchronized (fRunnablesProcessingJobLock) {
			if(fRunnablesProcessingJob != null) {
				fRunnablesProcessingJob.removePostponedRunnable(runnable);
			}
		}
	}
	
	/**
	 * <p>Internal {@link Job} for running all of the postponed runnables.</p>
	 * 
	 * <p><b>IMPORTANT:</b> Should never externally call schedule on this job, it deals with scheduling itself.</p>
	 *
	 */
	private static class RunnablesProcessingJob extends Job {

		/**
		 * <p>The current amount of time this job is waiting for before the job scheduler will run it.</p>
		 */
		private long fCurrentWaitTime;
		
		/**
		 * <p>The last time a call to {@link #reSchedule(long)} was made or -1 if the job has run
		 * since the last scheduling.</p>
		 */
		private long fTimeScheduled;

		/**
		 * <p>The postponed runnables to be run.</p>
		 */
		private final List fRunnables;
		
		/**
		 * <p>Use this lock whenever accessing {@link #fRunnables} or whenever changing the value of
		 * {@link #fTimeScheduled} or {@link #fCurrentWaitTime}.</p>
		 */
		private final Object LOCK = new Object();
		
		/**
		 * <p>Sets up this job as a long running system job</p>
		 */
		protected RunnablesProcessingJob() {
			super(Messages.PostponedRunnablesManager_job_title);
			
			//set this up as a long running system job
			this.setUser(false);
			this.setSystem(true);
			this.setPriority(Job.INTERACTIVE);
			
			this.fRunnables = new LinkedList();
			
			this.fCurrentWaitTime = -1;
			this.fTimeScheduled = -1;
		}
		
		/**
		 * <p>Adds an runnable to run using this job.</p>
		 * 
		 * @param runnable to run after the given delay
		 * @param delay to run the given runnable after
		 * 
		 * @return {@link PostponedRunnable} created from the given information, used if the runnable
		 * is to be canceled before it is run
		 * 
		 * {@link #removePostponedRunnable(Object)}
		 */
		protected PostponedRunnable addPostponedRunnable(ISafeRunnable runnable, int delay) {
			PostponedRunnable postponedAction = new PostponedRunnable(runnable, delay);
			this.addAction(postponedAction);
			
			return postponedAction;
		}
		
		/**
		 * <p>Removes an runnable to be run using this job.</p>
		 * 
		 * @param runnable {@link PostponedRunnable} that was created with {@link #addPostponedRunnable(ISafeRunnable, int)} or
		 * {@link #addAction(PostponedRunnable)} to remove before it is run
		 * 
		 * @throws InvalidParameterException if the given runnable was not the result of a
		 * call to {@link #addPostponedRunnable(ISafeRunnable, int)} or {@link #addAction(PostponedRunnable)}
		 */
		protected void removePostponedRunnable(Object runnable) throws IllegalArgumentException {
			if(runnable instanceof PostponedRunnable) {
				synchronized (LOCK) {
					//cancel it in case its already in the list to be processed by #run
					((PostponedRunnable)runnable).cancel();
					
					//remove from list of waiting runnables
					this.fRunnables.remove(runnable);
				}
			} else {
				throw new IllegalArgumentException("The given runnable to remove was not created " +
							"from a call to PostponedRunnablesManager#addPostponedAction");
			}
		}
		
		/**
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		protected IStatus run(IProgressMonitor monitor) {
			//if have runnables to run, try to run them
			if(this.hasRunnablesToRun()) {
				
				//get the current batch of runnables and try to run them
				List runnables = this.getRunnables();
				long shortestTimeToWait = 0;
				for(int i = 0; i < runnables.size(); ++i) {
					final PostponedRunnable runnable = (PostponedRunnable)runnables.get(i);
					
					//only try to run if not canceled, if it is canceled it will now just go away
					if(!runnable.isCanceled()) {
						/* if should run, then run
						 * else add back to runnables to run later
						 */
						if(runnable.doneWaiting()) {
							runnable.run();
						} else {
							long timeToWait = runnable.timeToWait();
							timeToWait = timeToWait < 0 ? 0 : timeToWait;
							shortestTimeToWait = shortestTimeToWait < timeToWait? shortestTimeToWait : timeToWait;
							this.addAction(runnable);
						}
					}
				}
				
				//if still more runnables to run, reschedule for the runnable with the shortest wait time, plus a bit
				if(this.hasRunnablesToRun()) {
					this.reSchedule(shortestTimeToWait + 100);
				}
			}
			
			return Status.OK_STATUS;
		}
		
		/**
		 * <p>Deals with re-scheduling this job to run again after a given amount of time.
		 * If the job has already been scheduled with a shorter wait time nothing happens.
		 * If the job has already been scheduled with a longer wait time the job is canceled and
		 * re-scheduled with the shorter wait time.
		 * If the job is already running it is scheduled to run again with the given wait time.</p>
		 * 
		 * @param newWaitTime minimum time before this job should run again
		 */
		private void reSchedule(long newWaitTime) {
			synchronized (LOCK) {
				//if not currently waiting or new wait time is less then approximate time left to wait then schedule
				long aproxTimeLeftToWait = System.currentTimeMillis() - this.fTimeScheduled - this.fCurrentWaitTime;
				if(this.fCurrentWaitTime == -1  || newWaitTime < aproxTimeLeftToWait) {
					/* cancel if already waiting or sleeping so we can reschedule at smaller wait time
					 * if job is running this cancel will do nothing because this jobs implementation of #run
					 * is designed to ignore cancel
					 */
					this.cancel();
					
					this.fCurrentWaitTime = newWaitTime;
					this.fTimeScheduled = System.currentTimeMillis();
					
					this.schedule(this.fCurrentWaitTime);
				} 
			}
		}
		
		/**
		 * <p>Adds an runnable back to the list of runnables to be run.  Helpful if an runnable
		 * still needs to wait longer before running.</p>
		 * 
		 * @return the given {@link PostponedRunnable}
		 */
		private void addAction(PostponedRunnable runnable) {
			synchronized (LOCK) {
				this.fRunnables.add(runnable);
			}
			
			this.reSchedule(runnable.fDelay);
		}
		
		/**
		 * @return <code>true</code> if there are runnables to be run,
		 * <code>false</code> if there are not.
		 */
		private boolean hasRunnablesToRun() {
			return !this.fRunnables.isEmpty();
		}
		
		/**
		 * <p>Creates a new list out of the existing runnables to run and then clears out
		 * {@link #fRunnables}.</p>
		 * 
		 * <p>This avoids a lot of the problems that would be caused by
		 * allowing runnables to be added to the same list being processed by {@link #run(IProgressMonitor)}.</p>
		 * 
		 * <p><b>IMPORTANT:</b> Because calling this removes the returned list of runnables from the master
		 * list of runnables any runnables that are not run now need to be added back to the master list.</p>
		 * 
		 * <p><b>NOTE:</b> Because the list returned is a new list the caller does not need to worry about
		 * synchronization when using the returned result.</p>
		 * 
		 * @return a snap shot of the runnables to be run
		 */
		private List getRunnables() {
			
			List runnables = null;
			synchronized (LOCK) {
				//reset current wait time
				this.fCurrentWaitTime = -1;
				this.fTimeScheduled = -1;
				
				runnables = new ArrayList(this.fRunnables);
				this.fRunnables.clear();
			}
			
			return runnables;
		}
		
		/**
		 * <p>Represents a single postponed runnable to be run after some amount of delay.</p>
		 */
		private static class PostponedRunnable {
			/** <p>The runnable to run after {@link #fDelay}. </p> */
			private final ISafeRunnable fRunnable;
			
			/** <p>Delay to wait before running this runnable.</p> */
			private final long fDelay;
			
			/** <p>Time this runnable was created.</p> */
			private final long fCreated;
			
			/** <code>true</code> if runnable canceled, <code>false</code> otherwise.</p> */
			private boolean fCanceled;
			
			/**
			 * <p>Creates an runnable to be run after a given delay.</p>
			 *
			 * @param runnable to run after the given delay
			 * @param delay to wait before running the given runnable
			 */
			public PostponedRunnable(ISafeRunnable runnable, long delay) {
				this.fRunnable = runnable;
				this.fDelay = delay;
				this.fCreated = System.currentTimeMillis();
				this.fCanceled = false;
			}
			
			/**
			 * @return <cod>true</code> if this runnable no longer needs to wait
			 * and can be run now, <code>false</code> if it still has more waiting to do
			 */
			protected boolean doneWaiting() {
				return this.timeToWait() <= 0;
			}
			
			/**
			 * @return amount of time to wait before this postponed runnable's delay is completed
			 */
			protected long timeToWait() {
				return this.fDelay - (System.currentTimeMillis() - this.fCreated);
			}
			
			/**
			 * <p>Prevents this postponed runnable from running assuming its not already running.
			 * If it is already running this is a no op.</p>
			 */
			protected void cancel() {
				this.fCanceled = true;
			}
			
			/**
			 * @return <code>true</code> if this postponed runnable has been canceled and thus
			 * should not run, <code>false</code> otherwise
			 */
			protected boolean isCanceled() {
				return this.fCanceled;
			}
			
			/**
			 * <p>Runs this postponed runnable using a {@link SafeRunner} so that any errors it may have
			 * will not make this entire system explode.</p>
			 */
			protected void run() {
				SafeRunner.run(this.fRunnable);
			}
		}
	}
}
