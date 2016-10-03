/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.interpret;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.wst.jsdt.internal.compiler.util.Util;

public class Interpreter {

	
	public static InterpreterResult interpet(String code, InterpreterContext context)
	{
		InterpretedScript parsedUnit = parseString(code);

		InterpreterResult result = interpret(parsedUnit.compilationUnit, context);
		
		parsedUnit.compilationUnit.cleanUp();
		
		return result;

	}


	public static InterpretedScript parseFile(String fileName) {
		File file = new File(fileName);
		InterpretedScript unit=null;
		if (file.exists())
		{
			try {
				byte[] fileByteContent = Util.getFileByteContent(file);
				char[] chars = Util.bytesToChar(fileByteContent, Util.UTF_8);
				unit=parse(chars);
			} catch (IOException e) {
			}
		}
		return unit;
	}
	
	public static InterpretedScript parseString(String code) {
		char[] source = code.toCharArray();
			return parse(source);
	}


	private static InterpretedScript parse(char[] source) {
		Map options =new CompilerOptions().getMap(); 
options.put(CompilerOptions.OPTION_ReportUnusedLocal, CompilerOptions.IGNORE);
		options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_3);
		options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_3);
		options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_1);
		CompilerOptions compilerOptions =new CompilerOptions(options);
Parser parser = 
		new Parser(
				new ProblemReporter(
						DefaultErrorHandlingPolicies.exitAfterAllProblems(),
						compilerOptions, 
						new DefaultProblemFactory(Locale.getDefault())),
			true); 

ICompilationUnit sourceUnit = new CompilationUnit(source, "interpreted", null); //$NON-NLS-1$

CompilationResult compilationUnitResult = new CompilationResult(sourceUnit, 0, 0,  compilerOptions.maxProblemsPerUnit);
CompilationUnitDeclaration parsedUnit = parser.parse(sourceUnit, compilationUnitResult);
int[] lineEnds = parser.scanner.lineEnds;
return new InterpretedScript(parsedUnit,lineEnds,parser.scanner.linePtr);
	}
	
	public static InterpreterResult interpret(CompilationUnitDeclaration ast, InterpreterContext context)
	{
	
		InterpreterEngine engine = new InterpreterEngine(context);
		
		return engine.interpret(ast);
		
	}
	
	
}
