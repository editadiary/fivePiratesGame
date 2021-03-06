package com.example.fivepiratesgame.game;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fivepiratesgame.Global;
import com.example.fivepiratesgame.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.socket.emitter.Emitter;

public class GameActivity extends AppCompatActivity {

    private List<PlayerData> playerList;
    private PlayerAdapter playerAdapter;
    private static RecyclerView rvPlayer;

    private LinearLayout voteLayout;

    private TextView tvName, tvbringGold, reject, accept;
    private ImageView avatar, refresh;


    private String userID;
    private String nickname;
    private int avatarID;
    private int roomID;
    private int initBringGold;

    ConstraintLayout introGame;
    ConstraintLayout inGame;
    TextView tvUserNum;
    int userNum;

    private final long finishtimeed = 1000;
    private long presstime = 0;

    boolean isEnd = false;


    public PlayerData me;
    public static GameActivity gameActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gameActivity = this;

        initGame();
        socket();

        //???????????? ????????? ??????
        reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(me.getVote() == -1) {
                    me.setVote(0);
                    Global.socket.emit("vote", roomID, 0);
                    reject.setBackgroundResource(R.drawable.game_reject);
                }
                //????????? ????????? ?????? ????????? ????????? ????????? ?????? ????????????
                //3??? ??? refresh
                else {
                    //?????? ?????? ????????? ??? ????????? ??????
                    //?????? ????????? ????????? ????????? ???????????????
                }
            }
        });

        //???????????? ????????? ??????
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(me.getVote() == -1) {
                    me.setVote(1);
                    Global.socket.emit("vote", roomID, 1);
                    accept.setBackgroundResource(R.drawable.game_accept);
                }
                //????????? ????????? ?????? ????????? ????????? ????????? ?????? ????????????
                else {
                }
            }
        });
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(me.getVote() != -1) {
                    Global.socket.emit("refresh", roomID, me.getVote());
                    me.setVote(-1);

                    accept.setBackgroundResource(R.drawable.game_textbox);
                    reject.setBackgroundResource(R.drawable.game_textbox);
                }
                //????????? ????????? ?????? ????????? ????????? ????????? ?????? ????????????
                else {
                }
            }
        });


    }

    @Override
    protected void onDestroy() {
        gameActivity = null;
        Global.socket.disconnect();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - presstime;

        if (0 <= intervalTime && finishtimeed >= intervalTime)
        {
            Global.socket.emit("disconnect_req", roomID, userID);
        }
        else
        {
            presstime = tempTime;
            Toast.makeText(getApplicationContext(), "??? ??? ??? ???????????? ??????????????? ????????????", Toast.LENGTH_SHORT).show();
        }
    }


    private void initGame() {
        playerList = new ArrayList<>();
        playerAdapter = new PlayerAdapter(GameActivity.this, playerList);
        rvPlayer = (RecyclerView) findViewById(R.id.rvPlayer);
        rvPlayer.setLayoutManager(new LinearLayoutManager(this));
        rvPlayer.setAdapter(playerAdapter);


        Intent intent = getIntent();
        userID = intent.getStringExtra("userID");
        nickname = intent.getStringExtra("nickname");
        avatarID = intent.getIntExtra("avatarID", 2);
        initBringGold = intent.getIntExtra("gold", 0);
        roomID = intent.getIntExtra("roomID", 0);

        introGame = findViewById(R.id.introGame);
        inGame = findViewById(R.id.inGame);
        tvUserNum = findViewById(R.id.tvUserNum);
        voteLayout = findViewById(R.id.voteLayout);
        reject = findViewById(R.id.reject);
        accept = findViewById(R.id.accept);
        refresh = findViewById(R.id.refresh);
        tvbringGold = findViewById(R.id.bringGold);


        introGame.setVisibility(View.VISIBLE);
        inGame.setVisibility(View.GONE);

        voteLayout.setVisibility(View.INVISIBLE);

    }

    private void socket() {

        //?????? ??????
        Global.socket.on("disconnect_req", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showEndDialog();
                    }
                });
            }
        });

        //?????? ?????? ??????????????? ????????? ?????? ????????? ????????? ??????, 5????????? ?????? ???????????? ??????
        Global.socket.on("join", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                userNum = (int) args[0];
                roomID = (int) args[1];

                tvUserNum.setText(Integer.toString(userNum) + " / 5");

                if (userNum == 5) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            introGame.setVisibility(View.GONE);
                            inGame.setVisibility(View.VISIBLE);
                        }
                    });
                }

            }
        });

        //?????? 5?????? ???????????? ????????? ?????? ?????? ???????????? RV??? ??????
        Global.socket.on("game_start", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject data ;

                    //?????? ???????????? ?????????
                    String tempUID;
                    String tempName;
                    int tempAID;
                    int tempOrder;

                    JSONArray dataArr = (JSONArray) ((JSONObject) args[0]).get("data");
                    playerList.clear();

                    for (int i = 0; i < dataArr.length(); i++) {
                        data = (JSONObject) dataArr.get(i);
                        tempUID = (String) data.get("user_id");
                        tempName = (String) data.get("nickname");
                        tempAID = (int) data.get("avatar_id");
                        tempOrder = (int) data.get("order");

                        PlayerData player = new PlayerData(tempUID, tempName, tempAID, tempOrder);
                        if (userID.equals(tempUID)) {
                            me = player;
                            me.setRoomId(roomID);
                            me.setBringGold(initBringGold);
                        }
                        playerList.add(player);
                    }


                    Comparator<PlayerData> comparator = new Comparator<PlayerData>() {
                        @Override
                        public int compare(PlayerData a, PlayerData b) {
                            return b.getOrder() - a.getOrder();
                        }
                    };

                    Collections.sort(playerList, comparator);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            playerAdapter.notifyDataSetChanged();
                            tvbringGold.setText(Integer.toString(me.getBringGold()));
                            if(me.getOrder() == (int) args[1]) { //?????? ??????1????????? sendoffer
                                sendOffer((int) args[1]);
                            }
                            else{ //??????1?????? ????????? ?????????
                                showWaitGDDialog(5);
                            }
                        }
                    });
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        //gold ?????? ????????? ????????? ????????? gold??? ????????? ??????
        Global.socket.on("offer_end", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONArray arr = (JSONArray) args[0];
                int myGold = 0;
                try {
                    myGold = (int) arr.get(me.getOrder());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                me.setGold(myGold);


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (me.getState() == 1) { voteLayout.setVisibility(View.VISIBLE); }
                        else {
                            PlayerData player;
                            for(int i = 0; i < playerList.size(); i++) {
                                player = playerList.get(i);
                                if(player.getState() == 1) {
                                    try {
                                        player.setGold((int) arr.get(player.getOrder()));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                else {
                                    player.setGold(-1);
                                }

                            }
                        }
                        playerAdapter.notifyDataSetChanged();
                    }
                });

            }
        });

        //?????? ?????? ????????? ?????? ?????? -> ???????????? ?????? bringGold??? ???
        Global.socket.on("offer_accept", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Global.socket.off("offer_accept");
                Global.socket.off("dead");
                if(me.getState() == 1) {
                    Global.socket.emit("game_end", roomID, userID, me.getBringGold());
                }
            }
       });

        Global.socket.on("dead", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Global.socket.off("offer_accept");
                Global.socket.off("dead");
                if(me.getState() == 1) {
                    me.setState(0);
                    Global.socket.emit("dead", roomID, userID);
                    voteLayout.setVisibility(View.INVISIBLE);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDeadDialog();
                            playerAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });

        Global.socket.on("offer_reject", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                for(int i = 0; i < playerList.size(); i++) {
                    if(playerList.get(i).getOrder() == (int) args[0] + 1) {
                        playerList.get(i).setState(0);
                    }
                }

//                if(me.getState() == 0) {
//                    voteLayout.setVisibility(View.INVISIBLE);
//                }

                me.setVote(-1);
                accept.setBackgroundResource(R.drawable.game_textbox);
                reject.setBackgroundResource(R.drawable.game_textbox);

                voteLayout.setVisibility(View.INVISIBLE);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(me.getOrder() == (int)args[0]) {
                            sendOffer((int)args[0]);
                            playerAdapter.notifyDataSetChanged();
                        }
                        else{ //??????1?????? ????????? ?????????
                            showWaitGDDialog((int)args[0]);
                        }
                    }
                });
            }
        });

        // ?????? ?????? -> ?????? disconnect
        Global.socket.on("game_end", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Global.socket.off("game_end");
                Global.socket.emit("disconnect_req", roomID, userID);
                // ?????? ???????
            }
        });

        Global.socket.on("msg", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("BBBBBBBBBBBBBBBB", (String) args[0]);

                for(int i = 0; i < playerList.size(); i++) {
                    if(playerList.get(i).getUserID().equals((String) args[0])) {
                        final int tempI = i;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showReceiveDialog(playerList.get(tempI).getNickname(), (int) args[1], (String) args[2]);
                            }
                        });
                        break;
                    }
                }

            }
        });


        Global.socket.on("dilemma", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if(me.getState() == 1 && me.getOrder() != 3) {
                    me.setVote(-1);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            accept.setBackgroundResource(R.drawable.game_textbox);
                            reject.setBackgroundResource(R.drawable.game_textbox);
                            showDilemmaDialog();
                        }
                    });
                }
            }
        });


        Global.socket.connect();

        Global.socket.emit("join", userID, initBringGold);

    }

    private void sendOffer(int num) {

        //?????? ????????? order?????? ????????? ?????? ????????? ???????????? ???????????????
        //??????????????? ?????? ????????????

        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);

        View view = LayoutInflater.from(GameActivity.this).inflate(
                R.layout.dialog_gold_distribution, (LinearLayout)findViewById(R.id.gdDialog));

        TextView tvleftCoin;
        EditText etG5, etG4,etG3, etG2, etG1;
        AppCompatButton btnConfirm;

        int leftCoin = 1000;
        builder.setView(view);

        tvleftCoin = view.findViewById(R.id.leftCoin);
        tvleftCoin.setText(String.valueOf(leftCoin));

        etG5 = view.findViewById(R.id.o5_gold);
        etG4 = view.findViewById(R.id.o4_gold);
        etG3 = view.findViewById(R.id.o3_gold);
        etG2 = view.findViewById(R.id.o2_gold);
        etG1 = view.findViewById(R.id.o1_gold);
        btnConfirm = view.findViewById(R.id.gdConfirm);

        if(num<4) {
            ((LinearLayout)view.findViewById(R.id.o4_layout)).setVisibility(View.GONE);
        }
        if(num<5) {
            ((LinearLayout)view.findViewById(R.id.o5_layout)).setVisibility(View.GONE);
        }

        AlertDialog dialog = builder.create();

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                int gold5, gold4, gold3, gold2, gold1;

                if(TextUtils.isEmpty(etG5.getText())) { gold5 = 0; }
                else gold5 = Integer.parseInt(etG5.getText().toString());

                if(TextUtils.isEmpty(etG4.getText())) { gold4 = 0; }
                else gold4 = Integer.parseInt(etG4.getText().toString());

                if(TextUtils.isEmpty(etG3.getText())) { gold3 = 0; }
                else gold3 = Integer.parseInt(etG3.getText().toString());

                if(TextUtils.isEmpty(etG2.getText())) { gold2 = 0; }
                else gold2 = Integer.parseInt(etG2.getText().toString());

                if(TextUtils.isEmpty(etG1.getText())) { gold1 = 0; }
                else gold1 = Integer.parseInt(etG1.getText().toString());

                int total = gold5 + gold4 + gold3 + gold2 + gold1;
                tvleftCoin.setText(String.valueOf(1000-total));

            }
        };
        etG5.addTextChangedListener(afterTextChangedListener);
        etG4.addTextChangedListener(afterTextChangedListener);
        etG3.addTextChangedListener(afterTextChangedListener);
        etG2.addTextChangedListener(afterTextChangedListener);
        etG1.addTextChangedListener(afterTextChangedListener);


        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int gold5, gold4, gold3, gold2, gold1;

                if(etG5.getText().equals("")) { gold5 = 0; }
                else gold5 = Integer.parseInt(etG5.getText().toString());

                if(etG4.getText().equals("")) { gold4 = 0; }
                else gold4 = Integer.parseInt(etG4.getText().toString());

                if(etG3.getText().equals("")) { gold3 = 0; }
                else gold3 = Integer.parseInt(etG3.getText().toString());

                if(etG2.getText().equals("")) { gold2 = 0; }
                else gold2 = Integer.parseInt(etG2.getText().toString());

                if(etG1.getText().equals("")) { gold1 = 0; }
                else gold1 = Integer.parseInt(etG1.getText().toString());

                int total = gold5 + gold4 + gold3 + gold2 + gold1;

                if (total != 1000){
                    Toast.makeText(getApplicationContext(), "????????? ????????? ?????? 1000?????? ????????????", Toast.LENGTH_LONG).show();
                }
                else {
                    switch (num){
                        case 3:
                            Global.socket.emit("offer", roomID, gold1, gold2, gold3);
                            break;
                        case 4:
                            Global.socket.emit("offer", roomID, gold1, gold2, gold3, gold4);
                            break;
                        case 5:
                            Global.socket.emit("offer", roomID, gold1, gold2, gold3, gold4, gold5);
                            break;
                    }
                    dialog.dismiss();
                }
            }
        });

        // Dialog ?????? ?????????
        if(dialog.getWindow() != null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        if (!GameActivity.this.isFinishing()) {
            dialog.show();
        }

    }
    private void showWaitGDDialog(int num){
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);

        View view = LayoutInflater.from(GameActivity.this).inflate(
                R.layout.dialog_waiting_gd, (LinearLayout)findViewById(R.id.waitGDDialog));

        builder.setView(view);

        TextView tvResult = (TextView) view.findViewById(R.id.tvResult);
        TextView tvWait = (TextView) view.findViewById(R.id.tvWait);
        TextView tvRound = (TextView) view.findViewById(R.id.tvRound);
        AppCompatButton btnConfirm = (AppCompatButton) view.findViewById(R.id.wgdConfirm);

        String tmpKing = "??????", King = "??????";

        switch (num){
            case 5: tmpKing = "??????"; King = "??????"; break;
            case 4: tmpKing = "??????"; King = "?????????"; break;
            case 3: tmpKing = "?????????"; King ="????????????"; break;
        }

        tvResult.setText(tmpKing + "??? ?????? ????????? ???????????? ????????????\n" +tmpKing+"??? ??????????????????");
        tvWait.setText(King + "??? ??????????????? ???????????????.");
        tvRound.setText("?????? ???????????? " + num + "????????????.");

        if(num == 5) tvResult.setVisibility(View.GONE);

        AlertDialog dialog = builder.create();

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        if(dialog.getWindow() != null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        if (!GameActivity.this.isFinishing()) {
            dialog.show();
        }

    }

    private void showDeadDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);

        View view = LayoutInflater.from(GameActivity.this).inflate(
                R.layout.dialog_dead, (LinearLayout)findViewById(R.id.deadDialog));

        builder.setView(view);

        AppCompatButton btnConfirm = (AppCompatButton) view.findViewById(R.id.wgdConfirm);

        AlertDialog dialog = builder.create();

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        if(dialog.getWindow() != null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        if (!GameActivity.this.isFinishing()) {
            dialog.show();
        }

    }

    private void showDilemmaDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);

        View view = LayoutInflater.from(GameActivity.this).inflate(
                R.layout.dialog_dilemma, (LinearLayout)findViewById(R.id.dmDialog));


        builder.setView(view);

        TextView dmGold = (TextView) view.findViewById(R.id.dmGold);
        AppCompatButton btnReject = (AppCompatButton) view.findViewById(R.id.dmReject);
        AppCompatButton btnAccept = (AppCompatButton) view.findViewById(R.id.dmAccept);

        AlertDialog dialog = builder.create();

        if(me.getOrder() == 2) {
            me.setGold(600);
        }
        else {
            me.setGold(400);
        }

        dmGold.setText(Integer.toString(me.getGold()) + " GOLD");


        btnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(me.getVote() == -1) {
                    me.setVote(0);
                    Global.socket.emit("dilemma", roomID, me.getUserID(), 0, me.getOrder());
                    dialog.dismiss();
                }
            }
        });

        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(me.getVote() == -1) {
                    me.setVote(1);
                    Global.socket.emit("dilemma", roomID, me.getUserID(), 1, me.getOrder());
                    dialog.dismiss();
                }
            }
        });

        if(dialog.getWindow() != null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        if (!GameActivity.this.isFinishing()) {
            dialog.show();
        }
    }
    public void showSendDialog(PlayerAdapter.PlayerViewHolder holder, int err_code) {

            // ???, ????????????, ?????? ???????????????
            if(err_code==1){
                Toast.makeText(getApplicationContext(), "???????????? ???????????? ????????? ??? ????????????", Toast.LENGTH_LONG).show();
                return;
            }
            if(err_code==2){
                Toast.makeText(getApplicationContext(), "????????? ???????????? ???????????? ????????? ??? ????????????", Toast.LENGTH_LONG).show();
                return;

            }
            if(me.getVote() != -1) {
                Toast.makeText(getApplicationContext(), "????????? ????????? ???????????? ???????????? ????????? ??? ????????????", Toast.LENGTH_LONG).show();
                return;

            }
            if(me.getBringGold() == 0) {
                Toast.makeText(getApplicationContext(), "???????????? ????????? ?????? ????????????", Toast.LENGTH_LONG).show();
                return;

            }
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);

        View view = LayoutInflater.from(GameActivity.this).inflate(
                R.layout.dialog_send_msg, (LinearLayout)findViewById(R.id.sendDialog));


        builder.setView(view);

        TextView myGold = (TextView) view.findViewById(R.id.myGold);
        EditText etSendGold = (EditText) view.findViewById(R.id.sendGold);
        EditText etSendMsg = (EditText) view.findViewById(R.id.sendMsg);
        AppCompatButton sendBtn = (AppCompatButton) view.findViewById(R.id.sendBtn);

        AlertDialog dialog = builder.create();

        myGold.setText(Integer.toString(me.getBringGold()));

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int sendGold;
                String sendMsg = etSendMsg.getText().toString();

                if(TextUtils.isEmpty(etSendGold.getText())) {
                    sendGold = 0;
                }

                else sendGold = Integer.parseInt(etSendGold.getText().toString());


                if (me.getBringGold() < sendGold) {
                    Toast.makeText(getApplicationContext(), "?????? ?????? ???????????? ?????? ????????? ?????? ??? ????????????", Toast.LENGTH_LONG).show();
                    sendGold = me.getBringGold();
                }

                me.setBringGold(me.getBringGold() - sendGold);

                tvbringGold.setText(Integer.toString(me.getBringGold()));

                Global.socket.emit("msg", me.getRoomId(), me.getUserID(), holder.getHolderUID(), sendGold, sendMsg);

                dialog.dismiss();

            }
        });

        if(dialog.getWindow() != null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        if (!GameActivity.this.isFinishing()) {
            dialog.show();
        }
    }

    public void showReceiveDialog(String sender, int receiveGold, String receiveMsg) {

        Log.d("AAAAAAAAAA", "AAAAAAAAAAAAA");

        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);

        View view = LayoutInflater.from(GameActivity.this).inflate(
                R.layout.dialog_receive_msg, (LinearLayout)findViewById(R.id.receiveDialog));


        builder.setView(view);

        TextView tvSender = (TextView) view.findViewById(R.id.sender);
        TextView tvReceiveGold = (TextView) view.findViewById(R.id.receiveGold);
        TextView tvReceiveMsg = (TextView) view.findViewById(R.id.receiveMsg);

        AppCompatButton btnConfirm = (AppCompatButton) view.findViewById(R.id.rmConfirm);


        AlertDialog dialog = builder.create();

        Log.d("AAAAAAAAAAA", Integer.toString(receiveGold));

        tvSender.setText(sender);
        tvReceiveGold.setText(Integer.toString(receiveGold));
        tvReceiveMsg.setText(receiveMsg);

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                me.setBringGold(me.getBringGold() + receiveGold);
                tvbringGold.setText(Integer.toString(me.getBringGold()));
                dialog.dismiss();
            }
        });

        if(dialog.getWindow() != null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        if (!GameActivity.this.isFinishing()) {
            dialog.show();
        }
    }


    private void showEndDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);

        View view = LayoutInflater.from(GameActivity.this).inflate(
                R.layout.dialog_game_end, (LinearLayout)findViewById(R.id.endDialog));

        builder.setView(view);

        TextView tvState = (TextView) view.findViewById(R.id.stateResult);
        TextView tvGold = (TextView) view.findViewById(R.id.goldResult);
        AppCompatButton btnConfirm = (AppCompatButton) view.findViewById(R.id.endConfirm);

        AlertDialog dialog = builder.create();

        if(me.getState() == 1){
            tvState.setText("???????????????! ??????????????? ????????? ???????????? ????????? ????????? ??????????????????!");
            int goldResult = me.getGold() - initBringGold + me.getBringGold();
            tvGold.setText("?????? : "+Integer.toString(goldResult));
        }
        else{
            tvState.setText("????????? ???????????? ???????????? ?????????????????????");
            tvGold.setText("?????? : " + Integer.toString(-initBringGold));
        }

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Global.socket.off("disconnect_req");
                Global.socket.off("game_end");
                Global.socket.off("dead");
                Global.socket.off("join");
                Global.socket.disconnect();
                dialog.dismiss();
                finish();
            }
        });

        if(dialog.getWindow() != null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        if (!GameActivity.this.isFinishing()) {
            dialog.show();
        }

    }


}