package com.dm.assist.chatgpt;

import android.content.Context;

import com.dm.assist.common.CharacterDialog;
import com.dm.assist.model.Campaign;
import com.dm.assist.model.OneShot;
import com.dm.assist.model.WorldCharacter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class ChatGPT
{
    Context context;
    public ChatGPT(Context c)
    {
        this.context = c.getApplicationContext();
    }

    private HttpURLConnection setupConnection() throws IOException, ExecutionException, InterruptedException {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return null;
        Task<GetTokenResult> task = user.getIdToken(false);
        Tasks.await(task);
        String url = "https://us-central1-dm-assist-381402.cloudfunctions.net/chatGPT";
        URL endpointURL = new URL(url);

        HttpURLConnection connection = (HttpURLConnection) endpointURL.openConnection();
        connection.setConnectTimeout(60000); // Set connection timeout to 60 seconds
        connection.setReadTimeout(60000); // Set read timeout to 60 seconds

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + task.getResult().getToken());
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return connection;
    }

    private void appendDmPrompt(Campaign c, StringBuilder promptSb)
    {
        promptSb.append("You are DungeonMind, you emulate celebrity Dungeon Masters to build the best campaign possible with your user\n\n");
    }

    private void appendSettingPrompt(Campaign c, StringBuilder promptSb)
    {
        promptSb.append("The setting is: ").append(c.desc).append("\n\n");
    }

    private void appendCharacterPrompts(List<WorldCharacter> chars, StringBuilder promptSb, String intro)
    {
        promptSb.append(intro);
        for (WorldCharacter wc: chars)
            promptSb.append(wc.name).append(": ").append(wc.description).append("\n");
        promptSb.append("\n\n");
    }
    private void appendCampaignPrompt(Campaign c, StringBuilder promptSb)
    {
        appendDmPrompt(c, promptSb);
        appendSettingPrompt(c, promptSb);
        appendCharacterPrompts(c.pcs, promptSb, "The PCs are: \n");

        // No need to send all the existing NPCs.
        // Let's just send a handful to save tokens.
        List<WorldCharacter> tempNPCs = new ArrayList<WorldCharacter>(c.npcs);
        Collections.shuffle(tempNPCs);
        tempNPCs = tempNPCs.subList(0, Math.min(tempNPCs.size(), 2));
        appendCharacterPrompts(tempNPCs, promptSb, "The NPCs are: \n");
    }

    private JSONObject writeGPTPrompt1msg(HttpURLConnection connection, String msg, String userMsg) throws JSONException, IOException {
        OutputStream outputStream = connection.getOutputStream();
        JSONObject root = new JSONObject();
        JSONArray messages = new JSONArray();
        JSONObject prompt = new JSONObject();
        prompt.put("role", "system");
        prompt.put("content", msg);
        messages.put(prompt);
        if (userMsg != null && userMsg.trim().length() > 0)
        {
            JSONObject userPrompt = new JSONObject();
            userPrompt.put("role", "user");
            userPrompt.put("content", userMsg.trim());
            messages.put(userPrompt);
        }

        root.put("model", "gpt-3.5-turbo");
        root.put("messages", messages);

        outputStream.write(root.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();

        return root;
    }

    private JSONObject writeGPTPromptCharacterDialog(HttpURLConnection connection, CharacterDialog dialog) throws JSONException, IOException
    {
        OutputStream outputStream = connection.getOutputStream();
        JSONObject root = new JSONObject();
        JSONArray messages = new JSONArray();
        JSONObject prompt = new JSONObject();
        prompt.put("role", "system");
        prompt.put("content", dialog.systemPrompt);
        messages.put(prompt);

        String role = "user";
        for (int i = 0; i < dialog.conversation.size(); i++)
        {
            prompt = new JSONObject();
            prompt.put("role", role);
            prompt.put("content", dialog.conversation.get(i));
            messages.put(prompt);

            if (role.equals("user"))
                role = "assistant";
            else
                role = "user";
        }

        root.put("model", "gpt-3.5-turbo");
        root.put("messages", messages);

        System.out.println("Writing Character Dialog");
        System.out.println(root.toString());

        outputStream.write(root.toString().getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();

        return root;
    }

    private String parseResponse(HttpURLConnection connection) throws IOException, JSONException {
        // Get the response code and response message
        int responseCode = connection.getResponseCode();
        String responseMessage = connection.getResponseMessage();

        // Print the response code and response message
        System.out.println("Response Code: " + responseCode);
        System.out.println("Response Message: " + responseMessage);

        // Read the response content
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer responseContent = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            responseContent.append(inputLine);
        }
        in.close();

        // Print the response content
        System.out.println("Response Content: " + responseContent.toString());

        JSONObject obj = new JSONObject(responseContent.toString());

        JSONArray choices = obj.getJSONArray("choices");
        JSONObject messageObj = choices.getJSONObject(0);
        JSONObject message = messageObj.getJSONObject("message");
        return message.getString("content");
    }

    private String[] parseNameDesc(String freetext, String defaultname)
    {
        String[] ss = freetext.split("\n");
        String name = "Unnamed NPC";
        String desc = freetext;

        int sum = 0;
        for (int i = 0; i < ss.length; i++)
        {
            if (ss[i].startsWith("Name:"))
                name = ss[i].substring("Name:".length()).trim();
            if (ss[i].startsWith("Description:"))
            {
                desc = freetext.substring(i + sum + "Description:".length()).trim();
                break;
            }
            sum += ss[i].length();
        }

        return new String[]{name, desc};
    }

    public WorldCharacter generateNPC(Campaign c, String npcPrompt) throws IOException, JSONException, ExecutionException, InterruptedException {
        System.out.println("Generating NPC...");
        HttpURLConnection connection = setupConnection();

        System.out.println("Building prompt...");
        StringBuilder sb = new StringBuilder();

        appendCampaignPrompt(c, sb);
        // Add task and parsing info
        sb.append("Generate a new NPC. Describe their short and long term goals, three one liners or zingers they might give that define their personality, and their relationships to the PCs and other NPCs and the world.");
        sb.append("\n\n");
        sb.append("Phrase your answer in the form\n" + "Name: \"...\"\n" + "Description: \"...\"");

        String completePrompt = sb.toString();

        JSONObject root = writeGPTPrompt1msg(connection, completePrompt, npcPrompt);
        System.out.println(root.toString(4));

        String content = parseResponse(connection);
        String[] namedesc = parseNameDesc(content, "Unnamed NPC");

        return new WorldCharacter(namedesc[0], namedesc[1]);
    }

    public OneShot generateOneShot(Campaign c) throws IOException, JSONException, ExecutionException, InterruptedException {
        HttpURLConnection connection = setupConnection();

        StringBuilder sb = new StringBuilder();
        appendCampaignPrompt(c, sb);
        // Add task and parsing info
        sb.append("Generate a one shot with plot hooks and dialog connecting the PCs to one or more of the NPCs or the setting. ");
        sb.append("Build at least 3 encounters, describe appropriate traps and obstacles, include one level appropriate magical item. ");
        sb.append("Supply example dialog discussing the situation and interacting with PCs for any npcs involved. ");
        sb.append("Supply 10 memorable zingers by the NPCs about the situation. Do not generate zingers for the PCs.");
        sb.append("\n\n");
        sb.append("Name the one shot after the involved characters and plot. Only a single One Shot should be generated. Phrase your one shot in the form\n" + "Name: \"...\"\n" + "Description: \"...\"");

        String completePrompt = sb.toString();

        JSONObject root = writeGPTPrompt1msg(connection, completePrompt, null);
        System.out.println(root.toString(4));

        String content = parseResponse(connection);
        String[] namedesc = parseNameDesc(content, "Unnamed NPC");

        return new OneShot(namedesc[0], namedesc[1]);
    }

    public CharacterDialog talkToCharacter(Campaign c, WorldCharacter wc, CharacterDialog activeDialog) throws IOException, JSONException, ExecutionException, InterruptedException {
        HttpURLConnection connection = setupConnection();

        StringBuilder system = new StringBuilder();
        appendDmPrompt(c, system);
        appendSettingPrompt(c, system);
        ArrayList<WorldCharacter> active = new ArrayList<WorldCharacter>();
        active.add(wc);
        appendCharacterPrompts(active, system, "You are being asked questions about the following character: ");

        system.append("Respond to user queries as either DungeonMind " + " or " + wc.name + " as appropriate.  Indicate who you are speaking as. ");

        String systemPrompt = system.toString();

        if (activeDialog == null)
            activeDialog = new CharacterDialog(systemPrompt, "Hi " + wc.name);

        writeGPTPromptCharacterDialog(connection, activeDialog);

        String content = parseResponse(connection);
        activeDialog.conversation.add(content);

        return activeDialog;
    }
}