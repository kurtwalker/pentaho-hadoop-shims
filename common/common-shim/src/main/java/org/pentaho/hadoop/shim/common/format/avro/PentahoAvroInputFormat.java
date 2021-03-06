/*******************************************************************************
 *
 * Pentaho Big Data
 *
 * Copyright (C) 2017 by Hitachi Vantara : http://www.pentaho.com
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
package org.pentaho.hadoop.shim.common.format.avro;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.commons.vfs2.FileExtensionSelector;
import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.hadoop.shim.api.format.IPentahoAvroInputFormat;
import org.pentaho.hadoop.shim.api.format.SchemaDescription;

public class PentahoAvroInputFormat implements IPentahoAvroInputFormat {

  private String fileName;
  private String schemaFileName;

  @Override
  public List<IPentahoInputSplit> getSplits() throws Exception {
    return null;
  }

  @Override
    public IPentahoRecordReader createRecordReader( IPentahoInputSplit split ) throws Exception {
    return new PentahoAvroRecordReader( createDataFileStream( schemaFileName, fileName ), readSchema( schemaFileName, fileName ) );
  }

  @Override
  public SchemaDescription readSchema( String schemaFileName, String fileName ) throws Exception {
    if ( schemaFileName != null && schemaFileName.length() > 0 ) {
      return AvroSchemaConverter.createSchemaDescription( readAvroSchema( schemaFileName ) );
    } else if ( fileName != null && fileName.length() > 0 ) {
      DataFileStream<GenericRecord> dataFileStream = createDataFileStream( schemaFileName, fileName );
      SchemaDescription schemaDescription = AvroSchemaConverter.createSchemaDescription( dataFileStream.getSchema() );
      dataFileStream.close();
      return  schemaDescription;
    } else {
      throw new Exception( "The file you provided does not contain a schema."
          + "  Please choose a schema file, or another file that contains a schema." );

    }
  }

  @Override
  public void setSchema( SchemaDescription schema ) throws Exception {
    //do nothing
  }

  @Override
  public void setInputFile( String fileName ) throws Exception {
    this.fileName = fileName;
  }

  @Override
  public void setInputSchemaFile( String schemaFileName ) throws Exception {
    this.schemaFileName = schemaFileName;
  }


  @Override
  public void setSplitSize( long blockSize ) throws Exception {
    //do nothing 
  }

  private Schema readAvroSchema( String schemaFile ) throws KettleFileException, IOException {
    return new Schema.Parser().parse( KettleVFS.getInputStream( schemaFile ) );
  }

  private DataFileStream<GenericRecord> createDataFileStream( String schemaFileName, String fileName ) throws Exception {
    DatumReader<GenericRecord> datumReader;
    if ( schemaFileName != null && schemaFileName.length() > 0 ) {
      datumReader = new GenericDatumReader<GenericRecord>( readAvroSchema( schemaFileName ) );
    } else {
      datumReader = new GenericDatumReader<GenericRecord>(  );
    }
    FileObject fileObject = KettleVFS.getFileObject( fileName );
    if ( fileObject.isFile() ) {
      return  new DataFileStream<GenericRecord>( fileObject.getContent().getInputStream(), datumReader );
    } else {
      ArrayList<InputStream> avroInputStreams = new ArrayList<InputStream>();
      FileObject[] avroFiles = fileObject.findFiles( new FileExtensionSelector( "avro" ) );
      for ( FileObject avroFile : avroFiles ) {
        avroInputStreams.add( avroFile.getContent().getInputStream() );
      }
      SequenceInputStream sis = new SequenceInputStream( Collections.enumeration( avroInputStreams ) );
      return  new DataFileStream<GenericRecord>( sis, datumReader );
    }
  }
}
