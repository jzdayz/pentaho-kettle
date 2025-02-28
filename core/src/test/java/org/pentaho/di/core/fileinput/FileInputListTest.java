/*******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2020 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package org.pentaho.di.core.fileinput;

import org.apache.commons.vfs2.FileObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.stubbing.Answer;
import org.pentaho.di.core.variables.VariableSpace;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by ccaspanello on 6/5/17.
 */
public class FileInputListTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  private static final String[] FIRST_LEVEL_FOLDERS = new String[] { "aa1", "aa2", "aa 3" };
  private static final String[] SECOND_LEVEL_FOLDERS = new String[] { "bb1", "bb2", "bb 3" };

  private static final int TOTAL_NUMBER_OF_FOLDERS_NO_ROOT = FIRST_LEVEL_FOLDERS.length // First level folders
    + ( FIRST_LEVEL_FOLDERS.length * SECOND_LEVEL_FOLDERS.length ); // Second level folders

  @Test
  public void testGetUrlStrings() throws Exception {
    String sFileA = "hdfs://myfolderA/myfileA.txt";
    String sFileB = "file:///myfolderB/myfileB.txt";

    FileObject fileA = mock( FileObject.class );
    FileObject fileB = mock( FileObject.class );

    when( fileA.getPublicURIString() ).thenReturn( sFileA );
    when( fileB.getPublicURIString() ).thenReturn( sFileB );

    FileInputList fileInputList = new FileInputList();
    fileInputList.addFile( fileA );
    fileInputList.addFile( fileB );

    String[] result = fileInputList.getUrlStrings();

    assertNotNull( result );
    assertEquals( 2, result.length );
    assertEquals( sFileA, result[ 0 ] );
    assertEquals( sFileB, result[ 1 ] );
  }

  @Test
  public void testSpecialCharsInFileNames() throws IOException {
    String fileNameWithSpaces = "file name with spaces";
    String fileNameWithPercents = "file%name%with%percents";
    String fileNameWithHash = "file#name#with#hashes";
    String fileNameWithGt = "file>name>with>greaterthan";
    String fileNameWithEverything = "file1%23name#with>everything%%";
    tempFolder.newFile( fileNameWithSpaces );
    tempFolder.newFile( fileNameWithPercents );
    tempFolder.newFile( fileNameWithHash );
    tempFolder.newFile( fileNameWithGt );
    tempFolder.newFile( fileNameWithEverything );

    VariableSpace spaceMock = mock( VariableSpace.class );
    when( spaceMock.environmentSubstitute( any( String[].class ) ) ).thenAnswer(
      (Answer<String[]>) invocationOnMock -> (String[]) invocationOnMock.getArguments()[ 0 ] );

    String[] folderNameList = { tempFolder.getRoot().getPath() };
    String[] emptyStringArray = { "" };

    boolean[] fileRequiredList = { true };
    String[] paths = FileInputList.createFilePathList( spaceMock, folderNameList, emptyStringArray, emptyStringArray, emptyStringArray, fileRequiredList );
    List<String> pathList = Arrays.asList( paths );
    assertTrue( "File with spaces not found", pathList.stream().anyMatch( p -> p.endsWith( fileNameWithSpaces ) ) );
    assertTrue( "File with percents not found", pathList.stream().anyMatch( p -> p.endsWith( fileNameWithPercents ) ) );
    assertTrue( "File with hashes not found", pathList.stream().anyMatch( p -> p.endsWith( fileNameWithHash ) ) );
    assertTrue( "File with greater than not found", pathList.stream().anyMatch( p -> p.endsWith( fileNameWithGt ) ) );
    assertTrue( "File with everything not found", pathList.stream().anyMatch( p -> p.endsWith( fileNameWithEverything ) ) );
    assertEquals( "Path array wrong size",  5, paths.length );
  }

  @Test
  public void testCreateFolderList() throws Exception {
    buildTestFolderTree();
    String[] folderNameList = { tempFolder.getRoot().getPath() };
    String[] folderRequiredList = { "N" };
    VariableSpace spaceMock = mock( VariableSpace.class );
    when( spaceMock.environmentSubstitute( any( String[].class ) ) ).thenAnswer(
      (Answer<String[]>) invocationOnMock -> (String[]) invocationOnMock.getArguments()[ 0 ] );

    FileInputList fileInputList = FileInputList.
      createFolderList( spaceMock, folderNameList, folderRequiredList );

    assertNotNull( fileInputList );
    assertEquals( TOTAL_NUMBER_OF_FOLDERS_NO_ROOT, fileInputList.nrOfFiles() );
  }

  /**
   * <p>Creates the following folder structure:</p>
   * <pre>
   *   \aa1
   *      \bb1
   *      \bb2
   *      \bb 3
   *   \aa2
   *      \bb1
   *      \bb2
   *      \bb 3
   *   \aa 3
   *      \bb1
   *      \bb2
   *      \bb 3
   * </pre>
   */
  private void buildTestFolderTree() throws IOException {
    for ( String folder1Name : FIRST_LEVEL_FOLDERS ) {
      // Create first level folders
      tempFolder.newFolder( folder1Name );

      for ( String folder2Name : SECOND_LEVEL_FOLDERS ) {
        // Create second level folders
        tempFolder.newFolder( folder1Name, folder2Name );
      }
    }
  }
}
