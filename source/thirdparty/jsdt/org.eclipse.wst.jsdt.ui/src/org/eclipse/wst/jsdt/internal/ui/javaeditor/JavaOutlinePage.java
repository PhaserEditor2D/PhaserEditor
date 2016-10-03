/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.ui.javaeditor;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.navigator.LocalSelectionTransfer;
import org.eclipse.wst.jsdt.core.ElementChangedEvent;
import org.eclipse.wst.jsdt.core.IClassFile;
import org.eclipse.wst.jsdt.core.IElementChangedListener;
import org.eclipse.wst.jsdt.core.IField;
import org.eclipse.wst.jsdt.core.IFunction;
import org.eclipse.wst.jsdt.core.IInitializer;
import org.eclipse.wst.jsdt.core.IJavaScriptElement;
import org.eclipse.wst.jsdt.core.IJavaScriptElementDelta;
import org.eclipse.wst.jsdt.core.IJavaScriptUnit;
import org.eclipse.wst.jsdt.core.IMember;
import org.eclipse.wst.jsdt.core.IParent;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.ISourceReference;
import org.eclipse.wst.jsdt.core.IType;
import org.eclipse.wst.jsdt.core.ITypeRoot;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;
import org.eclipse.wst.jsdt.internal.corext.util.JavaModelUtil;
import org.eclipse.wst.jsdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.wst.jsdt.internal.ui.IProductConstants;
import org.eclipse.wst.jsdt.internal.ui.JavaPluginImages;
import org.eclipse.wst.jsdt.internal.ui.JavaScriptPlugin;
import org.eclipse.wst.jsdt.internal.ui.ProductProperties;
import org.eclipse.wst.jsdt.internal.ui.actions.AbstractToggleLinkingAction;
import org.eclipse.wst.jsdt.internal.ui.actions.CategoryFilterActionGroup;
import org.eclipse.wst.jsdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.wst.jsdt.internal.ui.dnd.DelegatingDropAdapter;
import org.eclipse.wst.jsdt.internal.ui.dnd.JdtViewerDragAdapter;
import org.eclipse.wst.jsdt.internal.ui.packageview.PackagesMessages;
import org.eclipse.wst.jsdt.internal.ui.packageview.SelectionTransferDragAdapter;
import org.eclipse.wst.jsdt.internal.ui.packageview.SelectionTransferDropAdapter;
import org.eclipse.wst.jsdt.internal.ui.preferences.MembersOrderPreferenceCache;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.ColoredViewersManager;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.DecoratingJavaLabelProvider;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.SourcePositionComparator;
import org.eclipse.wst.jsdt.internal.ui.viewsupport.StatusBarUpdater;
import org.eclipse.wst.jsdt.ui.JavaScriptElementComparator;
import org.eclipse.wst.jsdt.ui.JavaScriptElementLabels;
import org.eclipse.wst.jsdt.ui.JavaScriptUI;
import org.eclipse.wst.jsdt.ui.PreferenceConstants;
import org.eclipse.wst.jsdt.ui.ProblemsLabelDecorator.ProblemsLabelChangedEvent;
import org.eclipse.wst.jsdt.ui.actions.CCPActionGroup;
import org.eclipse.wst.jsdt.ui.actions.CustomFiltersActionGroup;
import org.eclipse.wst.jsdt.ui.actions.GenerateActionGroup;
import org.eclipse.wst.jsdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.wst.jsdt.ui.actions.MemberFilterActionGroup;
import org.eclipse.wst.jsdt.ui.actions.OpenViewActionGroup;
import org.eclipse.wst.jsdt.ui.actions.RefactorActionGroup;


/**
 * The content outline page of the Java editor. The viewer implements a proprietary
 * update mechanism based on Java model deltas. It does not react on domain changes.
 * It is specified to show the content of ICompilationUnits and IClassFiles.
 * Publishes its context menu under <code>JavaScriptPlugin.getDefault().getPluginId() + ".outline"</code>.
 */
public class JavaOutlinePage extends Page implements IContentOutlinePage, IAdaptable , IPostSelectionProvider {

			static Object[] NO_CHILDREN= new Object[0];

			/**
			 * The element change listener of the java outline viewer.
			 * @see IElementChangedListener
			 */
			protected class ElementChangedListener implements IElementChangedListener {

				public void elementChanged(final ElementChangedEvent e) {

					if (getControl() == null)
						return;

					Display d= getControl().getDisplay();
					if (d != null) {
						d.asyncExec(new Runnable() {
							public void run() {
								IJavaScriptUnit cu= (IJavaScriptUnit) fInput;
								IJavaScriptElement base= cu;
								if (fTopLevelTypeOnly) {
									base= cu.findPrimaryType();
									if (base == null) {
										if (fOutlineViewer != null)
											fOutlineViewer.refresh(true);
										return;
									}
								}
								IJavaScriptElementDelta delta= findElement(base, e.getDelta());
								if (delta != null && fOutlineViewer != null) {
									fOutlineViewer.reconcile(delta);
								}
							}
						});
					}
				}

				private boolean isPossibleStructuralChange(IJavaScriptElementDelta cuDelta) {
					if (cuDelta.getKind() != IJavaScriptElementDelta.CHANGED) {
						return true; // add or remove
					}
					int flags= cuDelta.getFlags();
					if ((flags & IJavaScriptElementDelta.F_CHILDREN) != 0) {
						return true;
					}
					return (flags & (IJavaScriptElementDelta.F_CONTENT | IJavaScriptElementDelta.F_FINE_GRAINED)) == IJavaScriptElementDelta.F_CONTENT;
				}

				protected IJavaScriptElementDelta findElement(IJavaScriptElement unit, IJavaScriptElementDelta delta) {

					if (delta == null || unit == null)
						return null;

					IJavaScriptElement element= delta.getElement();

					if (unit.equals(element)) {
						if (isPossibleStructuralChange(delta)) {
							return delta;
						}
						return null;
					}


					if (element.getElementType() > IJavaScriptElement.CLASS_FILE)
						return null;

					IJavaScriptElementDelta[] children= delta.getAffectedChildren();
					if (children == null || children.length == 0)
						return null;

					for (int i= 0; i < children.length; i++) {
						IJavaScriptElementDelta d= findElement(unit, children[i]);
						if (d != null)
							return d;
					}

					return null;
				}
			}

			static class NoClassElement extends WorkbenchAdapter implements IAdaptable {
				/*
				 * @see java.lang.Object#toString()
				 */
				public String toString() {
					return JavaEditorMessages.JavaOutlinePage_error_NoTopLevelType;
				}

				/*
				 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(Class)
				 */
				public Object getAdapter(Class clas) {
					if (clas == IWorkbenchAdapter.class)
						return this;
					return null;
				}
			}

			/**
			 * Content provider for the children of an IJavaScriptUnit or
			 * an IClassFile
			 * @see ITreeContentProvider
			 */
			protected class ChildrenProvider implements ITreeContentProvider {

				private Object[] NO_CLASS= new Object[] {new NoClassElement()};
				private ElementChangedListener fListener;

				protected boolean matches(IJavaScriptElement element) {
					if (element.getElementType() == IJavaScriptElement.METHOD) {
						String name= element.getElementName();
						return (name != null && name.indexOf('<') >= 0);
					}
					
					//@GINO: Anonymous Filter top level anonymous
					if (element.getElementType() == IJavaScriptElement.TYPE && element.getParent().getElementType() == IJavaScriptElement.JAVASCRIPT_UNIT ) {
						
						IType type = (IType)element;
						try {
							return type.isAnonymous();
						} catch (JavaScriptModelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					return false;
				}

				protected IJavaScriptElement[] filter(IJavaScriptElement[] children) {
					boolean initializers= false;
					for (int i= 0; i < children.length; i++) {
						if (matches(children[i])) {
							initializers= true;
							break;
						}
					}

					if (!initializers)
						return children;

					Vector v= new Vector();
					for (int i= 0; i < children.length; i++) {
						if (matches(children[i]))
							continue;
						v.addElement(children[i]);
					}

					IJavaScriptElement[] result= new IJavaScriptElement[v.size()];
					v.copyInto(result);
					return result;
				}

				public Object[] getChildren(Object parent) {
					if (parent instanceof IParent) {
						IParent c= (IParent) parent;
						try {
							return filter(c.getChildren());
						} catch (JavaScriptModelException x) {
							// https://bugs.eclipse.org/bugs/show_bug.cgi?id=38341
							// don't log NotExist exceptions as this is a valid case
							// since we might have been posted and the element
							// removed in the meantime.
							if (JavaScriptPlugin.isDebug() || !x.isDoesNotExist())
								JavaScriptPlugin.log(x);
						}
					}
					return NO_CHILDREN;
				}

				public Object[] getElements(Object parent) {
					if (fTopLevelTypeOnly) {
						if (parent instanceof ITypeRoot) {
							try {
								IType type= ((ITypeRoot) parent).findPrimaryType();
								return type != null ? type.getChildren() : NO_CLASS;
							} catch (JavaScriptModelException e) {
								JavaScriptPlugin.log(e);
							}
						}
					}
					return getChildren(parent);
				}

				public Object getParent(Object child) {
					if (child instanceof IJavaScriptElement) {
						IJavaScriptElement e= (IJavaScriptElement) child;
						return e.getParent();
					}
					return null;
				}

				public boolean hasChildren(Object parent) {
					if (parent instanceof IParent) {
						IParent c= (IParent) parent;
						try {
							IJavaScriptElement[] children= filter(c.getChildren());
							return (children != null && children.length > 0);
						} catch (JavaScriptModelException x) {
							// https://bugs.eclipse.org/bugs/show_bug.cgi?id=38341
							// don't log NotExist exceptions as this is a valid case
							// since we might have been posted and the element
							// removed in the meantime.
							if (JavaScriptPlugin.isDebug() || !x.isDoesNotExist())
								JavaScriptPlugin.log(x);
						}
					}
					return false;
				}

				public boolean isDeleted(Object o) {
					return false;
				}

				public void dispose() {
					if (fListener != null) {
						JavaScriptCore.removeElementChangedListener(fListener);
						fListener= null;
					}
				}

				/*
				 * @see IContentProvider#inputChanged(Viewer, Object, Object)
				 */
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
					boolean isCU= (newInput instanceof IJavaScriptUnit);

					if (isCU && fListener == null) {
						fListener= new ElementChangedListener();
						JavaScriptCore.addElementChangedListener(fListener);
					} else if (!isCU && fListener != null) {
						JavaScriptCore.removeElementChangedListener(fListener);
						fListener= null;
					}
				}
			}

			/**
			 * The tree viewer used for displaying the outline.
			 *
			 * @see TreeViewer
			 */
			protected class JavaOutlineViewer extends TreeViewer {

				/**
				 * Indicates an item which has been reused. At the point of
				 * its reuse it has been expanded. This field is used to
				 * communicate between <code>internalExpandToLevel</code> and
				 * <code>reuseTreeItem</code>.
				 */
				private Item fReusedExpandedItem;
				private boolean fReorderedMembers;
				private boolean fForceFireSelectionChanged;

				public JavaOutlineViewer(Tree tree) {
					super(tree);
					setAutoExpandLevel(ALL_LEVELS);
					setUseHashlookup(true);
				}

				/**
				 * Investigates the given element change event and if affected
				 * incrementally updates the Java outline.
				 *
				 * @param delta the Java element delta used to reconcile the Java outline
				 */
				public void reconcile(IJavaScriptElementDelta delta) {
					fReorderedMembers= false;
					fForceFireSelectionChanged= false;
					if (getComparator() == null) {
						if (fTopLevelTypeOnly
							&& delta.getElement() instanceof IType
							&& (delta.getKind() & IJavaScriptElementDelta.ADDED) != 0)
						{
							refresh(true);

						} else {
							Widget w= findItem(fInput);
							if (w != null && !w.isDisposed())
								update(w, delta);
							if (fForceFireSelectionChanged)
								fireSelectionChanged(new SelectionChangedEvent(getSite().getSelectionProvider(), this.getSelection()));
							if (fReorderedMembers) {
								refresh(false);
								fReorderedMembers= false;
						}
						}
					} else {
						// just for now
						refresh(true);
					}
				}

				/*
				 * @see TreeViewer#internalExpandToLevel
				 */
				protected void internalExpandToLevel(Widget node, int level) {
					if (node instanceof Item) {
						Item i= (Item) node;
						if (i.getData() instanceof IJavaScriptElement) {
							IJavaScriptElement je= (IJavaScriptElement) i.getData();
							if (je.getElementType() == IJavaScriptElement.IMPORT_CONTAINER || isInnerType(je)) {
								if (i != fReusedExpandedItem) {
									setExpanded(i, false);
									return;
								}
							}
						}
					}
					super.internalExpandToLevel(node, level);
				}

				protected void reuseTreeItem(Item item, Object element) {

					// remove children
					Item[] c= getChildren(item);
					if (c != null && c.length > 0) {

						if (getExpanded(item))
							fReusedExpandedItem= item;

						for (int k= 0; k < c.length; k++) {
							if (c[k].getData() != null)
								disassociate(c[k]);
							c[k].dispose();
						}
					}

					updateItem(item, element);
					updatePlus(item, element);
					internalExpandToLevel(item, ALL_LEVELS);

					fReusedExpandedItem= null;
					fForceFireSelectionChanged= true;
				}

				protected boolean mustUpdateParent(IJavaScriptElementDelta delta, IJavaScriptElement element) {
					if (element instanceof IFunction) {
						if ((delta.getKind() & IJavaScriptElementDelta.ADDED) != 0) {
							return false;
						}
						return "main".equals(element.getElementName()); //$NON-NLS-1$
					}
					return false;
				}

				/*
				 * @see org.eclipse.jface.viewers.AbstractTreeViewer#isExpandable(java.lang.Object)
				 */
				public boolean isExpandable(Object element) {
					if (hasFilters()) {
						return getFilteredChildren(element).length > 0;
					}
					return super.isExpandable(element);
				}

				protected ISourceRange getSourceRange(IJavaScriptElement element) throws JavaScriptModelException {
					if (element instanceof ISourceReference)
						return ((ISourceReference) element).getSourceRange();
					if (element instanceof IMember && !(element instanceof IInitializer))
						return ((IMember) element).getNameRange();
					return null;
				}

				protected boolean overlaps(ISourceRange range, int start, int end) {
					return start <= (range.getOffset() + range.getLength() - 1) && range.getOffset() <= end;
				}

				protected boolean filtered(IJavaScriptElement parent, IJavaScriptElement child) {

					Object[] result= new Object[] { child };
					ViewerFilter[] filters= getFilters();
					for (int i= 0; i < filters.length; i++) {
						result= filters[i].filter(this, parent, result);
						if (result.length == 0)
							return true;
					}

					return false;
				}

				protected void update(Widget w, IJavaScriptElementDelta delta) {

					Item item;

					IJavaScriptElement parent= delta.getElement();
					IJavaScriptElementDelta[] affected= delta.getAffectedChildren();
					Item[] children= getChildren(w);

					boolean doUpdateParent= false;
					boolean doUpdateParentsPlus= false;

					Vector deletions= new Vector();
					Vector additions= new Vector();

					for (int i= 0; i < affected.length; i++) {
					    IJavaScriptElementDelta affectedDelta= affected[i];
						IJavaScriptElement affectedElement= affectedDelta.getElement();
						int status= affected[i].getKind();

						// find tree item with affected element
						int j;
						for (j= 0; j < children.length; j++)
						    if (affectedElement.equals(children[j].getData()))
						    	break;

						if (j == children.length) {
							// remove from collapsed parent
							if ((status & IJavaScriptElementDelta.REMOVED) != 0) {
								doUpdateParentsPlus= true;
								continue;
							}
							// addition
							if ((status & IJavaScriptElementDelta.CHANGED) != 0 &&
								(affectedDelta.getFlags() & IJavaScriptElementDelta.F_MODIFIERS) != 0 &&
								!filtered(parent, affectedElement))
							{
								additions.addElement(affectedDelta);
							}
							continue;
						}

						item= children[j];

						// removed
						if ((status & IJavaScriptElementDelta.REMOVED) != 0) {
							deletions.addElement(item);
							doUpdateParent= doUpdateParent || mustUpdateParent(affectedDelta, affectedElement);

						// changed
						} else if ((status & IJavaScriptElementDelta.CHANGED) != 0) {
							int change= affectedDelta.getFlags();
							doUpdateParent= doUpdateParent || mustUpdateParent(affectedDelta, affectedElement);

							if ((change & IJavaScriptElementDelta.F_MODIFIERS) != 0) {
								if (filtered(parent, affectedElement))
									deletions.addElement(item);
								else
									updateItem(item, affectedElement);
							}

							if ((change & IJavaScriptElementDelta.F_CONTENT) != 0)
								updateItem(item, affectedElement);

							if ((change & IJavaScriptElementDelta.F_CATEGORIES) != 0)
								updateItem(item, affectedElement);

							if ((change & IJavaScriptElementDelta.F_CHILDREN) != 0)
								update(item, affectedDelta);

							if ((change & IJavaScriptElementDelta.F_REORDER) != 0)
								fReorderedMembers= true;
						}
					}

					// find all elements to add
					IJavaScriptElementDelta[] add= delta.getAddedChildren();
					if (additions.size() > 0) {
						IJavaScriptElementDelta[] tmp= new IJavaScriptElementDelta[add.length + additions.size()];
						System.arraycopy(add, 0, tmp, 0, add.length);
						for (int i= 0; i < additions.size(); i++)
							tmp[i + add.length]= (IJavaScriptElementDelta) additions.elementAt(i);
						add= tmp;
					}

					// add at the right position
					go2: for (int i= 0; i < add.length; i++) {

						try {

							IJavaScriptElement e= add[i].getElement();
							if (filtered(parent, e))
								continue go2;

							doUpdateParent= doUpdateParent || mustUpdateParent(add[i], e);
							ISourceRange rng= getSourceRange(e);
							int start= rng.getOffset();
							int end= start + rng.getLength() - 1;
							int nameOffset= Integer.MAX_VALUE;
							if (e instanceof IField) {
								ISourceRange nameRange= ((IField) e).getNameRange();
								if (nameRange != null)
									nameOffset= nameRange.getOffset();
							}

							Item last= null;
							item= null;
							children= getChildren(w);

							for (int j= 0; j < children.length; j++) {
								item= children[j];
								IJavaScriptElement r= (IJavaScriptElement) item.getData();

								if (r == null) {
									// parent node collapsed and not be opened before -> do nothing
									continue go2;
								}


								try {
									rng= getSourceRange(r);

									// multi-field declarations always start at
									// the same offset. They also have the same
									// end offset if the field sequence is terminated
									// with a semicolon. If not, the source range
									// ends behind the identifier / initializer
									// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=51851
									boolean multiFieldDeclaration=
										r.getElementType() == IJavaScriptElement.FIELD
											&& e.getElementType() == IJavaScriptElement.FIELD
											&& rng.getOffset() == start;

									// elements are inserted by occurrence
									// however, multi-field declarations have
									// equal source ranges offsets, therefore we
									// compare name-range offsets.
									boolean multiFieldOrderBefore= false;
									if (multiFieldDeclaration) {
										if (r instanceof IField) {
											ISourceRange nameRange= ((IField) r).getNameRange();
											if (nameRange != null) {
												if (nameRange.getOffset() > nameOffset)
													multiFieldOrderBefore= true;
											}
										}
									}

									if (!multiFieldDeclaration && overlaps(rng, start, end)) {

										// be tolerant if the delta is not correct, or if
										// the tree has been updated other than by a delta
										reuseTreeItem(item, e);
										continue go2;

									} else if (multiFieldOrderBefore || rng.getOffset() > start) {

										if (last != null && deletions.contains(last)) {
											// reuse item
											deletions.removeElement(last);
											reuseTreeItem(last, e);
										} else {
											// nothing to reuse
											createTreeItem(w, e, j);
										}
										continue go2;
									}

								} catch (JavaScriptModelException x) {
									// stumbled over deleted element
								}

								last= item;
							}

							// add at the end of the list
							if (last != null && deletions.contains(last)) {
								// reuse item
								deletions.removeElement(last);
								reuseTreeItem(last, e);
							} else {
								// nothing to reuse
								createTreeItem(w, e, -1);
							}

						} catch (JavaScriptModelException x) {
							// the element to be added is not present -> don't add it
						}
					}


					// remove items which haven't been reused
					Enumeration e= deletions.elements();
					while (e.hasMoreElements()) {
						item= (Item) e.nextElement();
						disassociate(item);
						item.dispose();
					}

					if (doUpdateParent)
						updateItem(w, delta.getElement());
					if (!doUpdateParent && doUpdateParentsPlus && w instanceof Item)
						updatePlus((Item)w, delta.getElement());
				}



				/*
				 * @see ContentViewer#handleLabelProviderChanged(LabelProviderChangedEvent)
				 */
				protected void handleLabelProviderChanged(LabelProviderChangedEvent event) {
					Object input= getInput();
					if (event instanceof ProblemsLabelChangedEvent) {
						ProblemsLabelChangedEvent e= (ProblemsLabelChangedEvent) event;
						if (e.isMarkerChange() && input instanceof IJavaScriptUnit) {
							return; // marker changes can be ignored
						}
					}
					// look if the underlying resource changed
					Object[] changed= event.getElements();
					if (changed != null) {
						IResource resource= getUnderlyingResource();
						if (resource != null) {
							for (int i= 0; i < changed.length; i++) {
								if (changed[i] != null && changed[i].equals(resource)) {
									// change event to a full refresh
									event= new LabelProviderChangedEvent((IBaseLabelProvider) event.getSource());
									break;
								}
							}
						}
					}
					super.handleLabelProviderChanged(event);
				}

				private IResource getUnderlyingResource() {
					Object input= getInput();
					if (input instanceof IJavaScriptUnit) {
						IJavaScriptUnit cu= (IJavaScriptUnit) input;
						cu= cu.getPrimary();
						return cu.getResource();
					} else if (input instanceof IClassFile) {
						return ((IClassFile) input).getResource();
					}
					return null;
				}


			}

			class LexicalSortingAction extends Action {

				private JavaScriptElementComparator fComparator= new JavaScriptElementComparator();
				private SourcePositionComparator fSourcePositonComparator= new SourcePositionComparator();

				public LexicalSortingAction() {
					super();
					PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LEXICAL_SORTING_OUTLINE_ACTION);
					setText(JavaEditorMessages.JavaOutlinePage_Sort_label);
					JavaPluginImages.setLocalImageDescriptors(this, "alphab_sort_co.gif"); //$NON-NLS-1$
					setToolTipText(JavaEditorMessages.JavaOutlinePage_Sort_tooltip);
					setDescription(JavaEditorMessages.JavaOutlinePage_Sort_description);

					boolean checked= JavaScriptPlugin.getDefault().getPreferenceStore().getBoolean("LexicalSortingAction.isChecked"); //$NON-NLS-1$
					valueChanged(checked, false);
				}

				public void run() {
					valueChanged(isChecked(), true);
				}

				private void valueChanged(final boolean on, boolean store) {
					setChecked(on);
					BusyIndicator.showWhile(fOutlineViewer.getControl().getDisplay(), new Runnable() {
						public void run() {
							if (on)
								fOutlineViewer.setComparator(fComparator);
							else
								fOutlineViewer.setComparator(fSourcePositonComparator);
						}
					});

					if (store)
						JavaScriptPlugin.getDefault().getPreferenceStore().setValue("LexicalSortingAction.isChecked", on); //$NON-NLS-1$
				}
			}

		class ClassOnlyAction extends Action {

			public ClassOnlyAction() {
				super();
				PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.GO_INTO_TOP_LEVEL_TYPE_ACTION);
				setText(JavaEditorMessages.JavaOutlinePage_GoIntoTopLevelType_label);
				setToolTipText(JavaEditorMessages.JavaOutlinePage_GoIntoTopLevelType_tooltip);
				setDescription(JavaEditorMessages.JavaOutlinePage_GoIntoTopLevelType_description);
				JavaPluginImages.setLocalImageDescriptors(this, "gointo_toplevel_type.gif"); //$NON-NLS-1$

				IPreferenceStore preferenceStore= JavaScriptPlugin.getDefault().getPreferenceStore();
				boolean showclass= preferenceStore.getBoolean("GoIntoTopLevelTypeAction.isChecked"); //$NON-NLS-1$
				setTopLevelTypeOnly(showclass);
			}

			/*
			 * @see org.eclipse.jface.action.Action#run()
			 */
			public void run() {
				setTopLevelTypeOnly(!fTopLevelTypeOnly);
			}

			private void setTopLevelTypeOnly(boolean show) {
				fTopLevelTypeOnly= show;
				setChecked(show);
				fOutlineViewer.refresh(false);

				IPreferenceStore preferenceStore= JavaScriptPlugin.getDefault().getPreferenceStore();
				preferenceStore.setValue("GoIntoTopLevelTypeAction.isChecked", show); //$NON-NLS-1$
			}
		}

		private class CollapseAllAction extends Action {
			JavaOutlineViewer fJavaOutlineViewer;
			CollapseAllAction(JavaOutlineViewer viewer) {
				super(PackagesMessages.CollapseAllAction_label); 
				setDescription(PackagesMessages.CollapseAllAction_description); 
				setToolTipText(PackagesMessages.CollapseAllAction_tooltip); 
				JavaPluginImages.setLocalImageDescriptors(this, "collapseall.gif"); //$NON-NLS-1$
				
				fJavaOutlineViewer= viewer;
				PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.COLLAPSE_ALL_ACTION);
			}
		 
			public void run() { 
				fJavaOutlineViewer.collapseAll();
			}
		}


		/**
		 * This action toggles whether this Java Outline page links
		 * its selection to the active editor.
		 *
		 * 
		 */
		public class ToggleLinkingAction extends AbstractToggleLinkingAction {

			JavaOutlinePage fJavaOutlinePage;

			/**
			 * Constructs a new action.
			 *
			 * @param outlinePage the Java outline page
			 */
			public ToggleLinkingAction(JavaOutlinePage outlinePage) {
				boolean isLinkingEnabled= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE);
				setChecked(isLinkingEnabled);
				fJavaOutlinePage= outlinePage;
			}

			/**
			 * Runs the action.
			 */
			public void run() {
				PreferenceConstants.getPreferenceStore().setValue(PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE, isChecked());
				if (isChecked() && fEditor != null)
					fEditor.synchronizeOutlinePage(fEditor.computeHighlightRangeSourceReference(), false);
			}

		}

		/**
		 * Empty selection provider.
		 * 
		 * 
		 */
		private static final class EmptySelectionProvider implements ISelectionProvider {
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
			}
			public ISelection getSelection() {
				return StructuredSelection.EMPTY;
			}
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			}
			public void setSelection(ISelection selection) {
			}
		}


	/** A flag to show contents of top level type only */
	private boolean fTopLevelTypeOnly;

	private IJavaScriptElement fInput;
	private String fContextMenuID;
	private Menu fMenu;
	private JavaOutlineViewer fOutlineViewer;
	private JavaEditor fEditor;

	private MemberFilterActionGroup fMemberFilterActionGroup;

	private ListenerList fSelectionChangedListeners= new ListenerList(ListenerList.IDENTITY);
	private ListenerList fPostSelectionChangedListeners= new ListenerList(ListenerList.IDENTITY);
	private Hashtable fActions= new Hashtable();

	private TogglePresentationAction fTogglePresentation;

	private ToggleLinkingAction fToggleLinkingAction;

	private CompositeActionGroup fActionGroups;

	private IPropertyChangeListener fPropertyChangeListener;
	/**
	 * Custom filter action group.
	 * 
	 */
	private CustomFiltersActionGroup fCustomFiltersActionGroup;
	/**
	 * Category filter action group.
	 * 
	 */
	private CategoryFilterActionGroup fCategoryFilterActionGroup;

	public JavaOutlinePage(String contextMenuID, JavaEditor editor) {
		super();

		Assert.isNotNull(editor);

		fContextMenuID= contextMenuID;
		fEditor= editor;

		fTogglePresentation= new TogglePresentationAction();
		fTogglePresentation.setEditor(editor);

		fPropertyChangeListener= new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				doPropertyChange(event);
			}
		};
		JavaScriptPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(fPropertyChangeListener);
	}

	/* (non-Javadoc)
	 * Method declared on Page
	 */
	public void init(IPageSite pageSite) {
		super.init(pageSite);
	}

	private void doPropertyChange(PropertyChangeEvent event) {
		if (fOutlineViewer != null) {
			if (MembersOrderPreferenceCache.isMemberOrderProperty(event.getProperty())) {
				fOutlineViewer.refresh(false);
			}
		}
	}

	/*
	 * @see ISelectionProvider#addSelectionChangedListener(ISelectionChangedListener)
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		if (fOutlineViewer != null)
			fOutlineViewer.addSelectionChangedListener(listener);
		else
			fSelectionChangedListeners.add(listener);
	}

	/*
	 * @see ISelectionProvider#removeSelectionChangedListener(ISelectionChangedListener)
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		if (fOutlineViewer != null)
			fOutlineViewer.removeSelectionChangedListener(listener);
		else
			fSelectionChangedListeners.remove(listener);
	}

	/*
	 * @see ISelectionProvider#setSelection(ISelection)
	 */
	public void setSelection(ISelection selection) {
		if (fOutlineViewer != null)
			fOutlineViewer.setSelection(selection);
	}

	/*
	 * @see ISelectionProvider#getSelection()
	 */
	public ISelection getSelection() {
		if (fOutlineViewer == null)
			return StructuredSelection.EMPTY;
		return fOutlineViewer.getSelection();
	}

	/*
	 * @see org.eclipse.jface.text.IPostSelectionProvider#addPostSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
		if (fOutlineViewer != null)
			fOutlineViewer.addPostSelectionChangedListener(listener);
		else
			fPostSelectionChangedListeners.add(listener);
	}

	/*
	 * @see org.eclipse.jface.text.IPostSelectionProvider#removePostSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
		if (fOutlineViewer != null)
			fOutlineViewer.removePostSelectionChangedListener(listener);
		else
			fPostSelectionChangedListeners.remove(listener);
	}

	private void registerToolbarActions(IActionBars actionBars) {
		IToolBarManager toolBarManager= actionBars.getToolBarManager();
		toolBarManager.add(new LexicalSortingAction());

		fMemberFilterActionGroup= new MemberFilterActionGroup(fOutlineViewer, "org.eclipse.wst.jsdt.ui.JavaOutlinePage"); //$NON-NLS-1$
		fMemberFilterActionGroup.contributeToToolBar(toolBarManager);

		fCustomFiltersActionGroup.fillActionBars(actionBars);

		IMenuManager viewMenuManager= actionBars.getMenuManager();
		viewMenuManager.add(new Separator("EndFilterGroup")); //$NON-NLS-1$

		fToggleLinkingAction= new ToggleLinkingAction(this);
	//	viewMenuManager.add(new ClassOnlyAction());
		viewMenuManager.add(fToggleLinkingAction);
		toolBarManager.add(new CollapseAllAction(getOutlineViewer()));

		fCategoryFilterActionGroup= new CategoryFilterActionGroup(fOutlineViewer, "org.eclipse.wst.jsdt.ui.JavaOutlinePage", new IJavaScriptElement[] {fInput}); //$NON-NLS-1$
		fCategoryFilterActionGroup.contributeToViewMenu(viewMenuManager);
	}

	/*
	 * @see IPage#createControl
	 */
	public void createControl(Composite parent) {

		Tree tree= new Tree(parent, SWT.MULTI);

		AppearanceAwareLabelProvider lprovider= new AppearanceAwareLabelProvider(
			AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS |  JavaScriptElementLabels.F_APP_TYPE_SIGNATURE | JavaScriptElementLabels.ALL_CATEGORY,
			AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS
		);

		fOutlineViewer= new JavaOutlineViewer(tree);
		ColoredViewersManager.install(fOutlineViewer);
		initDragAndDrop();
		fOutlineViewer.setContentProvider(new ChildrenProvider());
		fOutlineViewer.setLabelProvider(new DecoratingJavaLabelProvider(lprovider));

		Object[] listeners= fSelectionChangedListeners.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			fSelectionChangedListeners.remove(listeners[i]);
			fOutlineViewer.addSelectionChangedListener((ISelectionChangedListener) listeners[i]);
		}

		listeners= fPostSelectionChangedListeners.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			fPostSelectionChangedListeners.remove(listeners[i]);
			fOutlineViewer.addPostSelectionChangedListener((ISelectionChangedListener) listeners[i]);
		}

		MenuManager manager= new MenuManager(fContextMenuID, fContextMenuID);
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager m) {
				contextMenuAboutToShow(m);
			}
		});
		fMenu= manager.createContextMenu(tree);
		tree.setMenu(fMenu);

		IPageSite site= getSite();
		site.registerContextMenu(JavaScriptPlugin.getPluginId() + ".outline", manager, fOutlineViewer); //$NON-NLS-1$
		
		updateSelectionProvider(site);
		
		// we must create the groups after we have set the selection provider to the site
		fActionGroups= new CompositeActionGroup(new ActionGroup[] {
				new OpenViewActionGroup(this),
				new CCPActionGroup(this),
				new GenerateActionGroup(this),
				new RefactorActionGroup(this),
				new JavaSearchActionGroup(this)});

		// register global actions
		IActionBars actionBars= site.getActionBars();
		actionBars.setGlobalActionHandler(ITextEditorActionConstants.UNDO, fEditor.getAction(ITextEditorActionConstants.UNDO));
		actionBars.setGlobalActionHandler(ITextEditorActionConstants.REDO, fEditor.getAction(ITextEditorActionConstants.REDO));

		IAction action= fEditor.getAction(ITextEditorActionConstants.NEXT);
		actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_NEXT_ANNOTATION, action);
		actionBars.setGlobalActionHandler(ITextEditorActionConstants.NEXT, action);
		action= fEditor.getAction(ITextEditorActionConstants.PREVIOUS);
		actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.GOTO_PREVIOUS_ANNOTATION, action);
		actionBars.setGlobalActionHandler(ITextEditorActionConstants.PREVIOUS, action);
		
		actionBars.setGlobalActionHandler(ITextEditorActionDefinitionIds.TOGGLE_SHOW_SELECTED_ELEMENT_ONLY, fTogglePresentation);

		fActionGroups.fillActionBars(actionBars);

		IStatusLineManager statusLineManager= actionBars.getStatusLineManager();
		if (statusLineManager != null) {
			StatusBarUpdater updater= new StatusBarUpdater(statusLineManager);
			fOutlineViewer.addPostSelectionChangedListener(updater);
		}
		// Custom filter group
		fCustomFiltersActionGroup= new CustomFiltersActionGroup("org.eclipse.wst.jsdt.ui.JavaOutlinePage", fOutlineViewer); //$NON-NLS-1$

		registerToolbarActions(actionBars);

		fOutlineViewer.setInput(fInput);
	}

	/*
	 * 
	 */
	private void updateSelectionProvider(IPageSite site) {
		ISelectionProvider provider= fOutlineViewer;
		if (fInput != null) {
			IJavaScriptUnit cu= (IJavaScriptUnit)fInput.getAncestor(IJavaScriptElement.JAVASCRIPT_UNIT);
			if (cu != null && !JavaModelUtil.isPrimary(cu))
				provider= new EmptySelectionProvider();
		}
		site.setSelectionProvider(provider);
	}

	public void dispose() {

		if (fEditor == null)
			return;

		if (fMemberFilterActionGroup != null) {
			fMemberFilterActionGroup.dispose();
			fMemberFilterActionGroup= null;
		}
		
		if (fCategoryFilterActionGroup != null) {
			fCategoryFilterActionGroup.dispose();
			fCategoryFilterActionGroup= null;
		}

		if (fCustomFiltersActionGroup != null) {
			fCustomFiltersActionGroup.dispose();
			fCustomFiltersActionGroup= null;
		}


		fEditor.outlinePageClosed();
		fEditor= null;

		fSelectionChangedListeners.clear();
		fSelectionChangedListeners= null;

		fPostSelectionChangedListeners.clear();
		fPostSelectionChangedListeners= null;

		if (fPropertyChangeListener != null) {
			JavaScriptPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(fPropertyChangeListener);
			fPropertyChangeListener= null;
		}

		if (fMenu != null && !fMenu.isDisposed()) {
			fMenu.dispose();
			fMenu= null;
		}

		if (fActionGroups != null)
			fActionGroups.dispose();

		fTogglePresentation.setEditor(null);

		fOutlineViewer= null;

		super.dispose();
	}

	public Control getControl() {
		if (fOutlineViewer != null)
			return fOutlineViewer.getControl();
		return null;
	}

	public void setInput(IJavaScriptElement inputElement) {
		fInput= inputElement;
		if (fOutlineViewer != null) {
			fOutlineViewer.setInput(fInput);
			updateSelectionProvider(getSite());
		}
		if (fCategoryFilterActionGroup != null) 
			fCategoryFilterActionGroup.setInput(new IJavaScriptElement[] {fInput});
	}

	public void select(ISourceReference reference) {
		if (fOutlineViewer != null) {

			ISelection s= fOutlineViewer.getSelection();
			if (s instanceof IStructuredSelection) {
				IStructuredSelection ss= (IStructuredSelection) s;
				List elements= ss.toList();
				if (!elements.contains(reference)) {
					s= (reference == null ? StructuredSelection.EMPTY : new StructuredSelection(reference));
					fOutlineViewer.setSelection(s, true);
				}
			}
		}
	}

	public void setAction(String actionID, IAction action) {
		Assert.isNotNull(actionID);
		if (action == null)
			fActions.remove(actionID);
		else
			fActions.put(actionID, action);
	}

	public IAction getAction(String actionID) {
		Assert.isNotNull(actionID);
		return (IAction) fActions.get(actionID);
	}

	/*
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class key) {
		if (key == IShowInSource.class) {
			return getShowInSource();
		}
		if (key == IShowInTargetList.class) {
			return new IShowInTargetList() {
				public String[] getShowInTargetIds() {
					String explorerViewID = ProductProperties.getProperty(IProductConstants.PERSPECTIVE_EXPLORER_VIEW);
					// make sure the specified view ID is known
					if (PlatformUI.getWorkbench().getViewRegistry().find(explorerViewID) == null)
						explorerViewID = IPageLayout.ID_PROJECT_EXPLORER;
					return new String[] {explorerViewID, JavaScriptUI.ID_PACKAGES };

				}

			};
		}
		if (key == IShowInTarget.class) {
			return getShowInTarget();
		}

		return null;
	}

	/**
	 * Convenience method to add the action installed under the given actionID to the
	 * specified group of the menu.
	 *
	 * @param menu		the menu manager
	 * @param group		the group to which to add the action
	 * @param actionID	the ID of the new action
	 */
	protected void addAction(IMenuManager menu, String group, String actionID) {
		IAction action= getAction(actionID);
		if (action != null) {
			if (action instanceof IUpdate)
				((IUpdate) action).update();

			if (action.isEnabled()) {
		 		IMenuManager subMenu= menu.findMenuUsingPath(group);
		 		if (subMenu != null)
		 			subMenu.add(action);
		 		else
		 			menu.appendToGroup(group, action);
			}
		}
	}

	protected void contextMenuAboutToShow(IMenuManager menu) {

		JavaScriptPlugin.createStandardGroups(menu);

		IStructuredSelection selection= (IStructuredSelection)getSelection();
		fActionGroups.setContext(new ActionContext(selection));
		fActionGroups.fillContextMenu(menu);
	}

	/*
	 * @see Page#setFocus()
	 */
	public void setFocus() {
		if (fOutlineViewer != null)
			fOutlineViewer.getControl().setFocus();
	}

	/**
	 * Checks whether a given Java element is an inner type.
	 *
	 * @param element the java element
	 * @return <code>true</code> iff the given element is an inner type
	 */
	private boolean isInnerType(IJavaScriptElement element) {

		if (element != null && element.getElementType() == IJavaScriptElement.TYPE) {
			IType type= (IType)element;
			try {
				return type.isMember();
			} catch (JavaScriptModelException e) {
				IJavaScriptElement parent= type.getParent();
				if (parent != null) {
					int parentElementType= parent.getElementType();
					return (parentElementType != IJavaScriptElement.JAVASCRIPT_UNIT && parentElementType != IJavaScriptElement.CLASS_FILE);
				}
			}
		}

		return false;
	}

	/**
	 * Returns the <code>IShowInSource</code> for this view.
	 *
	 * @return the {@link IShowInSource}
	 */
	protected IShowInSource getShowInSource() {
		return new IShowInSource() {
			public ShowInContext getShowInContext() {
				return new ShowInContext(
					null,
					getSite().getSelectionProvider().getSelection());
			}
		};
	}

	/**
	 * Returns the <code>IShowInTarget</code> for this view.
	 *
	 * @return the {@link IShowInTarget}
	 */
	protected IShowInTarget getShowInTarget() {
		return new IShowInTarget() {
			public boolean show(ShowInContext context) {
				ISelection sel= context.getSelection();
				if (sel instanceof ITextSelection) {
					ITextSelection tsel= (ITextSelection) sel;
					int offset= tsel.getOffset();
					IJavaScriptElement element= fEditor.getElementAt(offset);
					if (element != null) {
						setSelection(new StructuredSelection(element));
						return true;
					}
				} else if (sel instanceof IStructuredSelection) {
					setSelection(sel);
					return true;
				}
				return false;
			}
		};
	}

	private void initDragAndDrop() {
		int ops= DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;
		Transfer[] transfers= new Transfer[] {
			LocalSelectionTransfer.getInstance()
			};

		// Drop Adapter
		TransferDropTargetListener[] dropListeners= new TransferDropTargetListener[] {
			new SelectionTransferDropAdapter(fOutlineViewer)
		};
		fOutlineViewer.addDropSupport(ops | DND.DROP_DEFAULT, transfers, new DelegatingDropAdapter(dropListeners));

		// Drag Adapter
		TransferDragSourceListener[] dragListeners= new TransferDragSourceListener[] {
			new SelectionTransferDragAdapter(fOutlineViewer)
		};
		fOutlineViewer.addDragSupport(ops, transfers, new JdtViewerDragAdapter(fOutlineViewer, dragListeners));
	}
	
	/**
	 * Returns whether only the contents of the top level type is to be shown.
	 * 
	 * @return <code>true</code> if only the contents of the top level type is to be shown.
	 * 
	 */
	protected final boolean isTopLevelTypeOnly() {
		return fTopLevelTypeOnly;
	}
	
	/**
	 * Returns the <code>JavaOutlineViewer</code> of this view.
	 * 
	 * @return the {@link JavaOutlineViewer}
	 * 
	 */
	protected final JavaOutlineViewer getOutlineViewer() {
		return fOutlineViewer;
	}
}
