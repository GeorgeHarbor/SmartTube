package com.liskovsoft.smartyoutubetv2.common.exoplayer;
import android.net.Uri;
import com.google.android.exoplayer2.upstream.*;
import java.io.IOException;
import java.util.*;
/** Applies the same transport policy to OkHttp, Cronet, and platform media sources. */
public final class HttpsMediaDataSource implements DataSource {
    private final DataSource delegate;
    private HttpsMediaDataSource(DataSource delegate){this.delegate=delegate;}
    public static DataSource.Factory factory(DataSource.Factory factory){return ()->new HttpsMediaDataSource(factory.createDataSource());}
    @Override public void addTransferListener(TransferListener listener){delegate.addTransferListener(listener);}
    @Override public long open(DataSpec specification)throws IOException{
        String scheme=specification.uri.getScheme();
        if(!("https".equalsIgnoreCase(scheme) || "file".equalsIgnoreCase(scheme) || "content".equalsIgnoreCase(scheme) || "data".equalsIgnoreCase(scheme) || "asset".equalsIgnoreCase(scheme) || "rawresource".equalsIgnoreCase(scheme)))
            throw new IOException("Media network requests require HTTPS");
        return delegate.open(specification);
    }
    @Override public int read(byte[] buffer,int offset,int length)throws IOException{return delegate.read(buffer,offset,length);}
    @Override public Uri getUri(){return delegate.getUri();}
    @Override public Map<String,List<String>> getResponseHeaders(){return delegate.getResponseHeaders();}
    @Override public void close()throws IOException{delegate.close();}
}
