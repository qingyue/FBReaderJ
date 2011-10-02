/*
 * Copyright (C) 2010-2011 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.network;

import java.util.*;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;
import org.geometerplus.zlibrary.core.money.Money;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.android.util.UIUtil;

import org.geometerplus.fbreader.network.*;
import org.geometerplus.fbreader.network.tree.NetworkBookTree;
import org.geometerplus.fbreader.network.urlInfo.BookBuyUrlInfo;
import org.geometerplus.fbreader.network.authentication.NetworkAuthenticationManager;

import org.geometerplus.android.fbreader.network.*;

public class BuyBooksActivity extends Activity {
	public static void run(Activity activity, NetworkBookTree tree) {
		run(activity, Collections.singletonList(tree));
	}

	public static void run(Activity activity, List<NetworkBookTree> trees) {
		final Intent intent = new Intent(activity, BuyBooksActivity.class);
		final ArrayList<NetworkTree.Key> keys =
			new ArrayList<NetworkTree.Key>(trees.size());
		for (NetworkBookTree t : trees) {
			keys.add(t.getUniqueKey());
		}
		intent.putExtra(NetworkLibraryActivity.TREE_KEY_KEY, keys);
		activity.startActivity(intent);
	}

	private NetworkLibrary myLibrary;
	private INetworkLink myLink;
	private List<NetworkBookItem> myBooks;
	private Money myCost;
	private Money myAccount;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		Thread.setDefaultUncaughtExceptionHandler(new org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler(this));
		setContentView(R.layout.buy_book);

		myLibrary = NetworkLibrary.Instance();

		final List<NetworkTree.Key> keys =
			(List<NetworkTree.Key>)getIntent().getSerializableExtra(
				NetworkLibraryActivity.TREE_KEY_KEY
			);
		if (keys == null || keys.isEmpty()) {
			finish();
			return;
		}
		myBooks = new ArrayList<NetworkBookItem>(keys.size()); 
		for (NetworkTree.Key k : keys) {
			final NetworkTree tree = myLibrary.getTreeByKey(k);
			if (tree instanceof NetworkBookTree) {
				myBooks.add(((NetworkBookTree)tree).Book);
			} else {
				finish();
				return;
			}
		}
		myCost = Money.ZERO;
		for (NetworkBookItem b : myBooks) {
			if (b.getStatus() != NetworkBookItem.Status.CanBePurchased) {
				continue;
			}
			final BookBuyUrlInfo info = b.buyInfo();
			if (info == null || info.Price == null) {
				// TODO: error message
				finish();
				return;
			}
			myCost = myCost.add(info.Price);
		}

		// we assume all the books are from the same catalog
		myLink = myBooks.get(0).Link;
		final NetworkAuthenticationManager mgr = myLink.authenticationManager();
		if (mgr == null) {
			finish();
			return;
		}
		myAccount = mgr.currentAccount();

		setupUI();
	}

	private void setupUI() {
		final ZLResource dialogResource = ZLResource.resource("dialog");
		final ZLResource buttonResource = dialogResource.getResource("button");

		final TextView textArea = (TextView)findViewById(R.id.buy_book_text);
		final Button okButton =
			(Button)findViewById(R.id.buy_book_buttons).findViewById(R.id.ok_button);
		final Button cancelButton =
			(Button)findViewById(R.id.buy_book_buttons).findViewById(R.id.cancel_button);

		final ZLResource resource = ZLResource.resource("buyBook");
		if (myBooks.size() > 1) {
			setTitle(resource.getResource("titleSeveralBooks").getValue());
		} else {
			setTitle(resource.getResource("title").getValue());
		}

		if (myAccount == null) {
			textArea.setText(resource.getResource("noAccountInformation").getValue());
			okButton.setText(buttonResource.getResource("refresh").getValue());
			cancelButton.setText(buttonResource.getResource("cancel").getValue());
			okButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					refreshAccountInformation();
				} 
			});
			cancelButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					finish();
				} 
			});
		} else if (myCost.compareTo(myAccount) > 0) {
			textArea.setText(
				resource.getResource("unsufficientFunds").getValue()
					.replace("%0", myCost.toString())
					.replace("%1", myAccount.toString())
			);
			okButton.setText(buttonResource.getResource("pay").getValue());
			cancelButton.setText(buttonResource.getResource("refresh").getValue());
			okButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					TopupMenuActivity.runMenu(BuyBooksActivity.this, myLink, myCost.subtract(myAccount));
				} 
			});
			cancelButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					refreshAccountInformation();
				} 
			});
		} else {
			okButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					UIUtil.wait("purchaseBook", buyRunnable(), BuyBooksActivity.this);
				} 
			});
			cancelButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					finish();
				} 
			});
			if (myBooks.size() > 1) {
				textArea.setText(
					resource.getResource("confirmSeveralBooks").getValue()
						.replace("%s", String.valueOf(myBooks.size()))
				);
				okButton.setText(buttonResource.getResource("buy").getValue());
				cancelButton.setText(buttonResource.getResource("cancel").getValue());
			} else if (myBooks.get(0).getStatus() == NetworkBookItem.Status.CanBePurchased) {
				textArea.setText(
					resource.getResource("confirm").getValue().replace("%s", myBooks.get(0).Title)
				);
				okButton.setText(buttonResource.getResource("buy").getValue());
				cancelButton.setText(buttonResource.getResource("cancel").getValue());
			} else {
				textArea.setText(resource.getResource("alreadyBought").getValue());
				cancelButton.setText(buttonResource.getResource("ok").getValue());
				okButton.setVisibility(View.GONE);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshAccountInformation();
	}

	private void refreshAccountInformation() {
		UIUtil.wait(
			"updatingAccountInformation",
			new Runnable() {
				public void run() {
					final NetworkAuthenticationManager mgr = myLink.authenticationManager();
					try {
						mgr.refreshAccountInformation();
						final Money oldAccount = myAccount;
						myAccount = mgr.currentAccount();
						if (myAccount != null && !myAccount.equals(oldAccount)) {
							runOnUiThread(new Runnable() {
								public void run() {
									setupUI();
								}
							});
						}
						myLibrary.invalidateVisibility();
						myLibrary.synchronize();
					} catch (ZLNetworkException e) {
						// ignore
					}
				}
			},
			this
		);
	}

	private Runnable buyRunnable() {
		return new Runnable() {
			public void run() {
				try {
					final NetworkAuthenticationManager mgr = myLink.authenticationManager();
					for (final NetworkBookItem b : myBooks) {
						if (b.getStatus() != NetworkBookItem.Status.CanBePurchased) {
							continue;
						}
						mgr.purchaseBook(b);
						runOnUiThread(new Runnable() {
							public void run() {
								Util.doDownloadBook(BuyBooksActivity.this, b, false);
							}
						});
					}
					finish();
				} catch (final ZLNetworkException e) {
					final ZLResource dialogResource = ZLResource.resource("dialog");
					final ZLResource buttonResource = dialogResource.getResource("button");
					final ZLResource boxResource = dialogResource.getResource("networkError");
					runOnUiThread(new Runnable() {
						public void run() {
							new AlertDialog.Builder(BuyBooksActivity.this)
								.setTitle(boxResource.getResource("title").getValue())
								.setMessage(e.getMessage())
								.setIcon(0)
								.setPositiveButton(buttonResource.getResource("ok").getValue(), null)
								.create().show();
						}
					});
				} finally {
					myLibrary.invalidateVisibility();
					myLibrary.synchronize();
				}
			}
		};
	}
}