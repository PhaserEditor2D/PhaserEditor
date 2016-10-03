/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.codeassist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.wst.jsdt.core.compiler.CharOperation;
import org.eclipse.wst.jsdt.core.search.IJavaScriptSearchConstants;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayQualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.ArrayTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.TypeReference;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Binding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.wst.jsdt.internal.compiler.lookup.Scope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.wst.jsdt.internal.compiler.util.HashtableOfObjectToInt;
import org.eclipse.wst.jsdt.internal.core.SearchableEnvironment;

public class MissingTypesGuesser extends ASTVisitor {
	public static interface GuessedTypeRequestor {
		public void accept(
				TypeBinding guessedType,
				Binding[] missingElements,
				int[] missingElementsStarts,
				int[] missingElementsEnds,
				boolean hasProblems);

	}

	private static class ResolutionCleaner extends ASTVisitor {
		private HashtableOfObjectToInt bitsMap = new HashtableOfObjectToInt();
		private boolean firstCall = true;

		public ResolutionCleaner(){
			super();
		}

		private void cleanUp(TypeReference typeReference) {
			if (this.firstCall) {
				this.bitsMap.put(typeReference, typeReference.bits);
			} else {
				typeReference.bits = this.bitsMap.get(typeReference);
			}
			typeReference.resolvedType = null;
		}

		public void cleanUp(TypeReference convertedType, BlockScope scope) {
			convertedType.traverse(this, scope);
			this.firstCall = false;
		}

		public void cleanUp(TypeReference convertedType, ClassScope scope) {
			convertedType.traverse(this, scope);
			this.firstCall = false;
		}

		public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
			this.cleanUp(singleTypeReference);
			return true;
		}

		public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
			this.cleanUp(singleTypeReference);
			return true;
		}

		public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
			this.cleanUp(arrayTypeReference);
			return true;
		}

		public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
			this.cleanUp(arrayTypeReference);
			return true;
		}

		public boolean visit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
			this.cleanUp(qualifiedTypeReference);
			return true;
		}

		public boolean visit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
			this.cleanUp(qualifiedTypeReference);
			return true;
		}

		public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
			this.cleanUp(arrayQualifiedTypeReference);
			return true;
		}

		public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
			this.cleanUp(arrayQualifiedTypeReference);
			return true;
		}
	}

	private CompletionEngine.CompletionProblemFactory problemFactory ;
	private  SearchableEnvironment nameEnvironment;

	private HashMap substituedTypes;
	private HashMap originalTypes;
	private int combinationsCount;

	public MissingTypesGuesser(CompletionEngine completionEngine) {
		this.problemFactory = completionEngine.problemFactory;
		this.nameEnvironment = completionEngine.nameEnvironment;
	}

	private boolean computeMissingElements(
			QualifiedTypeReference[] substituedTypeNodes,
			char[][][] originalTypeNames,
			Binding[] missingElements,
			int[] missingElementsStarts,
			int[] missingElementsEnds) {
		int length = substituedTypeNodes.length;

		for (int i = 0; i < length; i++) {
			TypeReference substituedType = substituedTypeNodes[i];
			if (substituedType.resolvedType == null) return false;
			ReferenceBinding erasure = (ReferenceBinding)substituedType.resolvedType.leafComponentType();
			Binding missingElement;
			int depthToRemove = originalTypeNames[i].length - 1 ;
			if (depthToRemove == 0) {
				missingElement = erasure;
			} else {
				int depth = erasure.depth() + 1;

				if (depth > depthToRemove) {
					missingElement = erasure.enclosingTypeAt(depthToRemove);
				} else {
					return false;
					///////////////////////////////////////////////////////////
					//// Uncomment the following code to return missing package
					///////////////////////////////////////////////////////////
					//depthToRemove -= depth;
					//PackageBinding packageBinding = erasure.getPackage();
					//while(depthToRemove > 0) {
					//	packageBinding = packageBinding.parent;
					//	depthToRemove--;
					//}
					//missingElement = packageBinding;
				}
			}

			missingElements[i] = missingElement;
			missingElementsStarts[i] = substituedType.sourceStart;
			missingElementsEnds[i] = substituedType.sourceEnd + 1;

		}

		return true;
	}

	private TypeReference convert(ArrayQualifiedTypeReference typeRef) {
		if (typeRef.resolvedType != null) {
			if (typeRef.resolvedType.isValidBinding()) {
				ArrayQualifiedTypeReference convertedType =
					new ArrayQualifiedTypeReference(
							typeRef.tokens,
							typeRef.dimensions(),
							typeRef.sourcePositions);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.sourceEnd;
				return convertedType;
			} else if((typeRef.resolvedType.problemId() & ProblemReasons.NotFound) != 0) {
				// only the first token must be resolved
				if(((ReferenceBinding)typeRef.resolvedType.leafComponentType()).compoundName.length != 1) return null;

				char[][] typeName = typeRef.getTypeName();
				char[][][] typeNames = findTypeNames(typeName);
				if(typeNames == null || typeNames.length == 0) return null;
				ArrayQualifiedTypeReference convertedType =
					new ArrayQualifiedTypeReference(
							typeNames[0],
							typeRef.dimensions(),
							new long[typeNames[0].length]);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = (int)(typeRef.sourcePositions[0] & 0x00000000FFFFFFFFL);
				this.substituedTypes.put(convertedType, typeNames);
				this.originalTypes.put(convertedType, typeName);
				this.combinationsCount *= typeNames.length;
				return convertedType;
			}
		}
		return null;
	}

	private TypeReference convert(ArrayTypeReference typeRef) {
		if (typeRef.resolvedType != null) {
			if (typeRef.resolvedType.isValidBinding()) {
				ArrayTypeReference convertedType =
					new ArrayTypeReference(
							typeRef.token,
							typeRef.dimensions,
							0);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.originalSourceEnd;
				return convertedType;
			} else if((typeRef.resolvedType.problemId() & ProblemReasons.NotFound) != 0) {
				char[][] typeName = typeRef.getTypeName();
				char[][][] typeNames = findTypeNames(typeName);
				if(typeNames == null || typeNames.length == 0) return null;
				ArrayQualifiedTypeReference convertedType =
					new ArrayQualifiedTypeReference(
							typeNames[0],
							typeRef.dimensions,
							new long[typeNames[0].length]);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.originalSourceEnd;
				this.substituedTypes.put(convertedType, typeNames);
				this.originalTypes.put(convertedType, typeName);
				this.combinationsCount *= typeNames.length;
				return convertedType;
			}
		}
		return null;
	}

	private TypeReference convert(QualifiedTypeReference typeRef) {
		if (typeRef.resolvedType != null) {
			if (typeRef.resolvedType.isValidBinding()) {
				QualifiedTypeReference convertedType = new QualifiedTypeReference(typeRef.tokens, typeRef.sourcePositions);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.sourceEnd;
				return convertedType;
			} else if((typeRef.resolvedType.problemId() & ProblemReasons.NotFound) != 0) {
				// only the first token must be resolved
				if(((ReferenceBinding)typeRef.resolvedType).compoundName.length != 1) return null;

				char[][] typeName = typeRef.getTypeName();
				char[][][] typeNames = findTypeNames(typeName);
				if(typeNames == null || typeNames.length == 0) return null;
				QualifiedTypeReference convertedType = new QualifiedTypeReference(typeNames[0], new long[typeNames[0].length]);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = (int)(typeRef.sourcePositions[0] & 0x00000000FFFFFFFFL);
				this.substituedTypes.put(convertedType, typeNames);
				this.originalTypes.put(convertedType, typeName);
				this.combinationsCount *= typeNames.length;
				return convertedType;
			}
		}
		return null;
	}

	private TypeReference convert(SingleTypeReference typeRef) {
		if (typeRef.resolvedType != null) {
			if (typeRef.resolvedType.isValidBinding()) {
				SingleTypeReference convertedType = new SingleTypeReference(typeRef.token, 0);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.sourceEnd;
				return convertedType;
			} else if((typeRef.resolvedType.problemId() & ProblemReasons.NotFound) != 0) {
				char[][] typeName = typeRef.getTypeName();
				char[][][] typeNames = findTypeNames(typeName);
				if(typeNames == null || typeNames.length == 0) return null;
				QualifiedTypeReference convertedType = new QualifiedTypeReference(typeNames[0], new long[typeNames[0].length]);
				convertedType.sourceStart = typeRef.sourceStart;
				convertedType.sourceEnd = typeRef.sourceEnd;
				this.substituedTypes.put(convertedType, typeNames);
				this.originalTypes.put(convertedType, typeName);
				this.combinationsCount *= typeNames.length;
				return convertedType;
			}
		}
		return null;
	}

	private TypeReference convert(TypeReference typeRef) {
		if (typeRef instanceof ArrayTypeReference) {
			return convert((ArrayTypeReference)typeRef);
		} else if(typeRef instanceof ArrayQualifiedTypeReference) {
			return convert((ArrayQualifiedTypeReference)typeRef);
		} else if (typeRef instanceof SingleTypeReference) {
			return convert((SingleTypeReference)typeRef);
		} else if (typeRef instanceof QualifiedTypeReference) {
			return convert((QualifiedTypeReference)typeRef);
		}
		return null;
	}

	private char[][][] findTypeNames(char[][] missingTypeName) {
		char[] missingSimpleName = missingTypeName[missingTypeName.length - 1];
		final boolean isQualified = missingTypeName.length > 1;
		final char[] missingFullyQualifiedName =
			isQualified ? CharOperation.concatWith(missingTypeName, '.') : null;
		final ArrayList results = new ArrayList();
		ISearchRequestor storage = new ISearchRequestor() {

			public void acceptPackage(char[] packageName) {
				// package aren't searched
			}
			public void acceptType(
					char[] packageName,
					char[] fileName,
					char[] typeName,
					char[][] enclosingTypeNames,
					int modifiers,
					AccessRestriction accessRestriction) {
				char[] fullyQualifiedName = CharOperation.concat(packageName, CharOperation.concat(CharOperation.concatWith(enclosingTypeNames, '.'), typeName, '.'), '.');
				if (isQualified && !CharOperation.endsWith(fullyQualifiedName, missingFullyQualifiedName)) return;
				char[][] compoundName = CharOperation.splitOn('.', fullyQualifiedName);
				results.add(compoundName);
			}
			public void acceptBinding(char[] packageName, char[] fileName, char[] bindingName, int bindingType, int modifiers, AccessRestriction accessRestriction) {
				//do nothing
			}
			
			/**
			 * @see org.eclipse.wst.jsdt.internal.codeassist.ISearchRequestor#acceptConstructor(
			 * 		int, char[], char[][], char[][], java.lang.String, org.eclipse.wst.jsdt.internal.compiler.env.AccessRestriction)
			 */
			public void acceptConstructor(int modifiers, char[] typeName,
					char[][] parameterTypes, char[][] parameterNames,
					String path, AccessRestriction access) {
				
				//do nothing
			}
			
			/**
			 * @see org.eclipse.wst.jsdt.internal.codeassist.ISearchRequestor#acceptFunction(char[], char[][], char[][], char[], char[], char[], char[], int, java.lang.String)
			 */
			public void acceptFunction(char[] signature, char[][] parameterFullyQualifedTypeNames,
						char[][] parameterNames, char[] returnQualification, char[] returnSimpleName,
						char[] declaringQualification, char[] declaringSimpleName, int modifiers, String path) {
				
				//do nothing
			}
			
			/**
			 * @see org.eclipse.wst.jsdt.internal.codeassist.ISearchRequestor#acceptVariable(char[], char[], char[], char[], char[], int, java.lang.String)
			 */
			public void acceptVariable(char[] signature,
					char[] typeQualification, char[] typeSimpleName,
					char[] declaringQualification, char[] declaringSimpleName,
					int modifiers, String path) {
				
				//do nothing
			}
		};
		nameEnvironment.findExactTypes(missingSimpleName, true, IJavaScriptSearchConstants.TYPE, storage);
		if(results.size() == 0) return null;
		return (char[][][])results.toArray(new char[results.size()][0][0]);
	}

	private char[][] getOriginal(TypeReference typeRef) {
		return (char[][])this.originalTypes.get(typeRef);
	}

	private QualifiedTypeReference[] getSubstituedTypes() {
		Set types = this.substituedTypes.keySet();
		return (QualifiedTypeReference[]) types.toArray(new QualifiedTypeReference[types.size()]);
	}

	private char[][][] getSubstitution(TypeReference typeRef) {
		return (char[][][])this.substituedTypes.get(typeRef);
	}

	public void guess(TypeReference typeRef, Scope scope, GuessedTypeRequestor requestor) {
		this.substituedTypes = new HashMap();
		this.originalTypes = new HashMap();
		this.combinationsCount = 1;

		TypeReference convertedType = convert(typeRef);

		if(convertedType == null) return;

		QualifiedTypeReference[] substituedTypeNodes = this.getSubstituedTypes();
		int length = substituedTypeNodes.length;

		int[] substitutionsIndexes = new int[substituedTypeNodes.length];
		char[][][][] subtitutions = new char[substituedTypeNodes.length][][][];
		char[][][] originalTypeNames = new char[substituedTypeNodes.length][][];
		for (int i = 0; i < substituedTypeNodes.length; i++) {
			subtitutions[i] = this.getSubstitution(substituedTypeNodes[i]);
			originalTypeNames[i] = this.getOriginal(substituedTypeNodes[i]);
		}

		ResolutionCleaner resolutionCleaner = new ResolutionCleaner();
		for (int i = 0; i < this.combinationsCount; i++) {

			nextSubstitution(substituedTypeNodes, subtitutions, substitutionsIndexes);


			this.problemFactory.startCheckingProblems();
			TypeBinding guessedType = null;
			switch (scope.kind) {
				case Scope.METHOD_SCOPE :
				case Scope.BLOCK_SCOPE :
					resolutionCleaner.cleanUp(convertedType, (BlockScope)scope);
					guessedType = convertedType.resolveType((BlockScope)scope);
					break;
				case Scope.CLASS_SCOPE :
					resolutionCleaner.cleanUp(convertedType, (ClassScope)scope);
					guessedType = convertedType.resolveType((ClassScope)scope);
					break;
			}
			this.problemFactory.stopCheckingProblems();
			if (!this.problemFactory.hasForbiddenProblems) {
				if (guessedType != null) {
					Binding[] missingElements = new Binding[length];
					int[] missingElementsStarts = new int[length];
					int[] missingElementsEnds = new int[length];

					if(computeMissingElements(
							substituedTypeNodes,
							originalTypeNames,
							missingElements,
							missingElementsStarts,
							missingElementsEnds)) {
						requestor.accept(
								guessedType,
								missingElements,
								missingElementsStarts,
								missingElementsEnds,
								this.problemFactory.hasAllowedProblems);
					}
				}
			}
		}
	}
	private void nextSubstitution(
			QualifiedTypeReference[] substituedTypeNodes,
			char[][][][] subtitutions,
			int[] substitutionsIndexes) {
		int length = substituedTypeNodes.length;

		done : for (int i = 0; i < length; i++) {
			if(substitutionsIndexes[i] < subtitutions[i].length - 1) {
				substitutionsIndexes[i]++;
				break done;
			} else {
				substitutionsIndexes[i] = 0;
			}
		}

		for (int i = 0; i < length; i++) {
			QualifiedTypeReference qualifiedTypeReference = substituedTypeNodes[i];
			qualifiedTypeReference.tokens = subtitutions[i][substitutionsIndexes[i]];
			qualifiedTypeReference.sourcePositions = new long[qualifiedTypeReference.tokens.length];
		}
	}
}
