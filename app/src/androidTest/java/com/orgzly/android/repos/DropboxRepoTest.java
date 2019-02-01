package com.orgzly.android.repos;

import com.orgzly.BuildConfig;
import com.orgzly.android.BookName;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.db.entity.BookView;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.util.MiscUtils;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DropboxRepoTest extends OrgzlyTest {
    private static final String DROPBOX_TEST_DIR = "/orgzly-android-tests";

    @Before
    public void setUp() throws Exception {
        super.setUp();

        AppPreferences.dropboxToken(context, BuildConfig.DROPBOX_TOKEN);
    }

    @Test
    public void testUrl() {
        assertEquals("dropbox:/dir", repoFactory.getFromUri(context, "dropbox:/dir").getUri().toString());
    }

    @Test
    public void testSyncingUrlWithTrailingSlash() throws IOException {
        testUtils.setupRepo(randomUrl() + "/");
        assertNotNull(testUtils.sync());
    }

    /* Dropbox repo url should *not* have authority. */
    @Test
    public void testAuthority() {
        assertNull(repoFactory.getFromUri(context, "dropbox://authority"));
    }

    @Test
    public void testRenameBook() throws IOException {
        BookView bookView;
        String repoUriString = repoFactory.getFromUri(context, randomUrl()).getUri().toString();

        testUtils.setupRepo(repoUriString);
        testUtils.setupBook("booky", "");

        testUtils.sync();
        bookView = dataRepository.getBookView("booky");

        assertEquals(repoUriString, bookView.getLinkedTo());
        assertEquals(repoUriString, bookView.getSyncedTo().getRepoUri().toString());
        assertEquals(repoUriString + "/booky.org", bookView.getSyncedTo().getUri().toString());

        dataRepository.renameBook(bookView, "booky-renamed");
        bookView = dataRepository.getBookView("booky-renamed");

        assertEquals(repoUriString, bookView.getLinkedTo());
        assertEquals(repoUriString, bookView.getSyncedTo().getRepoUri().toString());
        assertEquals(repoUriString + "/booky-renamed.org", bookView.getSyncedTo().getUri().toString());
    }

    @Test
    public void testDropboxFileRename() throws IOException {
        SyncRepo repo = repoFactory.getFromUri(context, randomUrl());

        assertNotNull(repo);
        assertEquals(0, repo.getBooks().size());

        File file = File.createTempFile("notebook.", ".org");
        MiscUtils.writeStringToFile("1 2 3", file);

        VersionedRook vrook = repo.storeBook(file, file.getName());

        assertEquals(1, repo.getBooks().size());

        repo.renameBook(vrook.getUri(), "notebook-renamed");

        assertEquals(1, repo.getBooks().size());
        assertEquals(repo.getUri() + "/notebook-renamed.org", repo.getBooks().get(0).getUri().toString());
        assertEquals("notebook-renamed.org", BookName.getInstance(context, repo.getBooks().get(0)).getFileName());
    }

    private String randomUrl() {
        return "dropbox:"+ DROPBOX_TEST_DIR + "/" + UUID.randomUUID().toString();
    }
}
