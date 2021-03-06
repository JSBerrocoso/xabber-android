/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomInvite;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.ui.adapter.AccountChooseAdapter;
import com.xabber.android.ui.helper.BarPainter;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;

import org.jivesoftware.smack.util.StringUtils;

import java.util.Collection;

public class MUCEditor extends ManagedActivity implements OnItemSelectedListener {

    private static final String SAVED_ACCOUNT = "com.xabber.android.ui.MUCEditor.SAVED_ACCOUNT";
    private static final String SAVED_ROOM = "com.xabber.android.ui.MUCEditor.SAVED_ROOM";

    private String account;
    private String room;

    /**
     * Last selected account.
     */
    private int selectedAccount;

    /**
     * Views.
     */
    private Spinner accountView;
    private EditText serverView;
    private EditText roomView;
    private EditText nickView;
    private EditText passwordView;

    private BarPainter barPainter;

    public static Intent createIntent(Context context) {
        return MUCEditor.createIntent(context, null, null);
    }

    public static Intent createIntent(Context context, String account,
                                      String room) {
        return new EntityIntentBuilder(context, MUCEditor.class).setAccount(account).setUser(room).build();
    }

    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static String getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.muc_editor);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
        setTitle(null);

        setSupportActionBar(toolbar);

        barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();

        accountView = (Spinner) findViewById(R.id.contact_account);
        serverView = (EditText) findViewById(R.id.muc_server);
        roomView = (EditText) findViewById(R.id.muc_room);
        nickView = (EditText) findViewById(R.id.muc_nick);
        passwordView = (EditText) findViewById(R.id.muc_password);

        accountView.setAdapter(new AccountChooseAdapter(this));
        accountView.setOnItemSelectedListener(this);

        Intent intent = getIntent();
        if (savedInstanceState != null) {
            account = savedInstanceState.getString(SAVED_ACCOUNT);
            room = savedInstanceState.getString(SAVED_ROOM);
        } else {
            account = getAccount(intent);
            room = getUser(intent);
            if (room != null) {
                serverView.setText(StringUtils.parseServer(room));
                roomView.setText(StringUtils.parseName(room));
            }
            if (account != null && room != null) {
                MUCManager.getInstance()
                        .removeAuthorizationError(account, room);
                nickView.setText(MUCManager.getInstance().getNickname(account,
                        room));
                String password;
                RoomInvite roomInvite = MUCManager.getInstance().getInvite(
                        account, room);
                if (roomInvite != null) {
                    password = roomInvite.getPassword();
                } else {
                    password = MUCManager.getInstance().getPassword(account, room);
                }
                passwordView.setText(password);
            }
        }
        if (account == null) {
            Collection<String> accounts = AccountManager.getInstance().getAccounts();
            if (accounts.size() == 1) {
                account = accounts.iterator().next();
            }
        }

        if (account != null) {
            barPainter.updateWithAccountName(account);

            for (int position = 0; position < accountView.getCount(); position++) {
                if (account.equals(accountView.getItemAtPosition(position))) {
                    accountView.setSelection(position);
                    break;
                }
            }
        }
        if ("".equals(nickView.getText().toString())) {
            nickView.setText(getNickname(((String) accountView.getSelectedItem())));
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        account = (String) accountView.getSelectedItem();
        outState.putString(SAVED_ACCOUNT, account);
        outState.putString(SAVED_ROOM, room);
    }

    @Override
    protected void onResume() {
        super.onResume();
        selectedAccount = accountView.getSelectedItemPosition();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.add_conference, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_conference:
                addConference();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addConference() {
        String account = (String) accountView.getSelectedItem();
        if (account == null) {
            Toast.makeText(this, getString(R.string.EMPTY_ACCOUNT), Toast.LENGTH_LONG).show();
            return;
        }
        String server = serverView.getText().toString();
        if ("".equals(server)) {
            Toast.makeText(this, getString(R.string.EMPTY_SERVER_NAME), Toast.LENGTH_LONG).show();
            return;
        }
        String room = roomView.getText().toString();
        if ("".equals(room)) {
            Toast.makeText(this, getString(R.string.EMPTY_ROOM_NAME), Toast.LENGTH_LONG).show();
            return;
        }
        String nick = nickView.getText().toString();
        if ("".equals(nick)) {
            Toast.makeText(this, getString(R.string.EMPTY_NICK_NAME), Toast.LENGTH_LONG).show();
            return;
        }
        String password = passwordView.getText().toString();
        boolean join = ((CheckBox) findViewById(R.id.muc_join)).isChecked();
        room = room + "@" + server;
        if (this.account != null && this.room != null) {
            if (!account.equals(this.account) || !room.equals(this.room)) {
                MUCManager.getInstance().removeRoom(this.account, this.room);
                MessageManager.getInstance().closeChat(this.account, this.room);
                NotificationManager.getInstance().removeMessageNotification(this.account, this.room);
            }
        }
        MUCManager.getInstance().createRoom(account, room, nick, password, join);
        finish();
    }

    /**
     * @param account
     * @return Suggested nickname in the room.
     */
    private String getNickname(String account) {
        if (account == null) {
            return "";
        }
        String nickname = AccountManager.getInstance().getNickName(account);
        String name = StringUtils.parseName(nickname);
        if ("".equals(name)) {
            return nickname;
        } else {
            return name;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String current = nickView.getText().toString();
        String previous;
        if (selectedAccount == AdapterView.INVALID_POSITION) {
            previous = "";
        } else {
            previous = getNickname((String) accountView.getAdapter().getItem(selectedAccount));
        }
        if (current.equals(previous)) {
            nickView.setText(getNickname((String) accountView.getSelectedItem()));
        }
        selectedAccount = accountView.getSelectedItemPosition();

        barPainter.updateWithAccountName((String) accountView.getSelectedItem());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        selectedAccount = accountView.getSelectedItemPosition();
    }
}
