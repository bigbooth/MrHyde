package org.faudroids.mrhyde.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Optional;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.app.MrHydeApp;
import org.faudroids.mrhyde.jekyll.Draft;
import org.faudroids.mrhyde.jekyll.Post;
import org.faudroids.mrhyde.ui.utils.JekyllUiUtils;

import java.io.File;
import java.util.List;

import rx.Observable;

public class PostsActivity extends AbstractJekyllActivity<Post> {

	public PostsActivity() {
		super(
				R.string.posts,
				R.string.no_posts,
				R.string.action_unpublish_post,
				R.string.post_unpublished,
				R.string.unpublish_post_title,
				R.string.unpublish_post_message);
	}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    ((MrHydeApp) getApplication()).getComponent().inject(this);
    super.onCreate(savedInstanceState);
  }

	@Override
	protected void onAddClicked(JekyllUiUtils.OnContentCreatedListener<Post> contentListener) {
    jekyllUiUtils.showNewPostDialog(this, jekyllManager, repository, Optional.<File>absent(), contentListener);
  }

	@Override
	protected Observable<List<Post>> doLoadItems() {
		return jekyllManager.getAllPosts();
	}

	@Override
	protected AbstractAdapter createAdapter() {
		return new PostsAdapter();
	}

	@Override
	protected Observable<Draft> createMoveObservable(Post post) {
		return jekyllManager.unpublishPost(post);
	}

	@Override
	protected String getMovedFilenameForItem(Post post) {
		return "_drafts/" + jekyllManager.draftTitleToFilename(post.getTitle());
	}

	public class PostsAdapter extends AbstractAdapter {

		@Override
		public AbstractViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_overview_post, parent, false);
			return new PostViewHolder(view);
		}

		public class PostViewHolder extends AbstractViewHolder {

			public PostViewHolder(View view) {
				super(view);
			}

			@Override
			protected void doSetItem(Post item) {
				jekyllUiUtils.setPostOverview(view, item);
			}

		}
	}

}
