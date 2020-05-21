package org.eclipse.transformer.maven;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TransformMojoTest {

	@Test
	@Ignore
	public void testTransformerPlugin() throws Exception {
		File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/transform-build-artifact");

		final Verifier verifier;

		verifier = new Verifier( testDir.getAbsolutePath() );
//		verifier.deleteArtifact( "org.apache.maven.its.itsample", "parent", "1.0", "pom" );
//		verifier.deleteArtifact( "org.apache.maven.its.itsample", "checkstyle-test", "1.0", "jar" );
//		verifier.deleteArtifact( "org.apache.maven.its.itsample", "checkstyle-assembly", "1.0", "jar" );

		final List<String> cliOptions = new ArrayList<>();
		cliOptions.add( "-N" );
		verifier.setCliOptions( cliOptions );
		verifier.executeGoal( "install" );

		verifier.verifyErrorFreeLog();
		verifier.resetStreams();
	}



}
