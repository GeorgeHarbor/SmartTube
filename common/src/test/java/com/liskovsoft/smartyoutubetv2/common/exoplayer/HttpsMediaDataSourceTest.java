package com.liskovsoft.smartyoutubetv2.common.exoplayer;
import android.net.Uri;
import com.google.android.exoplayer2.upstream.*;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=23)
public class HttpsMediaDataSourceTest {
    private static class Fake implements DataSource {
        boolean opened;
        public void addTransferListener(TransferListener ignored){}
        public long open(DataSpec spec){opened=true;return 0;}
        public int read(byte[] buffer,int offset,int length){return -1;}
        public Uri getUri(){return null;}
        public void close(){}
    }
    @Test public void cleartextIsRejectedBeforeTheNetworkSourceOpens()throws Exception{
        Fake fake=new Fake();DataSource source=HttpsMediaDataSource.factory(()->fake).createDataSource();
        try{source.open(new DataSpec(Uri.parse("http://media.invalid/video")));fail("Cleartext accepted");}catch(IOException expected){}
        assertFalse(fake.opened);
    }
    @Test public void httpsReachesSelectedPlaybackSource()throws Exception{
        Fake fake=new Fake();DataSource source=HttpsMediaDataSource.factory(()->fake).createDataSource();
        source.open(new DataSpec(Uri.parse("https://media.invalid/video")));assertTrue(fake.opened);
    }
}
