package org.faudroids.mrhyde.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.app.MrHydeApp;
import org.faudroids.mrhyde.git.GitManager;
import org.faudroids.mrhyde.git.RepositoriesManager;
import org.faudroids.mrhyde.git.Repository;
import org.faudroids.mrhyde.jekyll.PreviewManager;
import org.faudroids.mrhyde.ui.utils.AbstractActivity;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class PreviewActivity extends AbstractActivity {

  private static final String STATE_URL = "STATE_URL";

  static final String EXTRA_REPO = "EXTRA_REPO";

  @Inject RepositoriesManager repositoriesManager;
  @Inject PreviewManager previewManager;
  @BindView(R.id.web_view) protected WebView webView;

  private String currentUrl;

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @Override
  public void onCreate(Bundle savedInstanceState) {
    ((MrHydeApp) getApplication()).getComponent().inject(this);
    super.onCreate(savedInstanceState);
    setTitle(getString(R.string.title_preview));
    setContentView(R.layout.activity_preview);
    ButterKnife.bind(this);

    // load arguments
    Repository repository = (Repository) getIntent().getSerializableExtra(EXTRA_REPO);
    GitManager gitManager = repositoriesManager.openRepository(repository);

    // setup preview view
    webView.getSettings().setJavaScriptEnabled(true);
    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public boolean onConsoleMessage(ConsoleMessage message) {
        Timber.d(message.message());
        return true;
      }
    });
    webView.setWebViewClient(new WebViewClient() {
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return false;
      }

      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        currentUrl = url;
        super.onPageStarted(view, url, favicon);
      }
    });

    // load preview
    if (savedInstanceState != null) {
      currentUrl = savedInstanceState.getString(STATE_URL);
      webView.restoreState(savedInstanceState);

    } else {
      showSpinner();
      compositeSubscription.add(previewManager
          .loadPreview(gitManager)
          .compose(new DefaultTransformer<>())
          .subscribe(previewUrl1 -> {
                Timber.d("getting url " + previewUrl1);
                webView.loadUrl(previewUrl1);
                currentUrl = previewUrl1;
                invalidateOptionsMenu();
                hideSpinner();
              },
              new ErrorActionBuilder()
                  .add(new DefaultErrorAction(this, "failed to get preview from server"))
                  .add(new HideSpinnerAction(this))
                  .build()));
    }
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_preview, menu);
    return true;
  }


  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.action_open_in_browser).setVisible(currentUrl != null);
    return true;
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_open_in_browser:
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.setData(Uri.parse(currentUrl));
        startActivity(browserIntent);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }


  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putSerializable(STATE_URL, currentUrl);
    webView.saveState(outState);
  }


  @Override
  public void onBackPressed() {
    WebBackForwardList backForwardList = webView.copyBackForwardList();
    if (backForwardList.getCurrentIndex() > 1) webView.goBack();
    else super.onBackPressed();
  }

}
