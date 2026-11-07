package dev.mlml.korppu.imgui.windows;

import dev.mlml.korppu.command.Command;
import dev.mlml.korppu.command.CommandManager;
import imgui.ImGui;
import imgui.ImGuiInputTextCallbackData;
import imgui.callback.ImGuiInputTextCallback;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConsoleWindow extends ImGuiWindow {
    private static ConsoleWindow instance;

    private final List<String> log = new ArrayList<>();
    private final List<String> cmdHistory = new ArrayList<>();
    private int historyIdx = -1;
    private final ImString inputBuf = new ImString(512);
    private final ImBoolean openBool = new ImBoolean(true);
    private static final int MAX_SUGGESTIONS = 20;
    private List<String> currentSuggestions = List.of();
    private boolean suggestionsTruncated = false;
    private String lastSuggestionInput = null;
    private boolean scrollToBottom = false;
    private boolean reclaimFocus = false;

    public ConsoleWindow() {
        instance = this;
    }

    public static void print(String message) {
        if (instance == null) return;
        instance.log.addAll(Arrays.asList(message.split("\n", -1)));
        instance.scrollToBottom = true;
    }

    @Override
    public void render() {
        if (!visible) return;

        openBool.set(true);
        ImGui.setNextWindowSize(520, 420, ImGuiCond.Once);
        ImGui.begin("Console##korppu_console", openBool);

        float suggestionH = currentSuggestions.isEmpty() ? 0
                : Math.min(currentSuggestions.size(), 8) * ImGui.getTextLineHeightWithSpacing() + 6;
        float footerH = ImGui.getStyle().getItemSpacingY() + ImGui.getFrameHeightWithSpacing();

        ImGui.beginChild("##clog", 0, -(footerH + suggestionH), false,
                ImGuiWindowFlags.HorizontalScrollbar);
        for (String line : log) {
            ImGui.textUnformatted(line);
        }
        if (scrollToBottom) {
            ImGui.setScrollHereY(1.0f);
            scrollToBottom = false;
        }
        ImGui.endChild();

        if (!currentSuggestions.isEmpty()) {
            int show = Math.min(currentSuggestions.size(), 8);
            int rows = show + (suggestionsTruncated ? 1 : 0);
            ImGui.beginChild("##csugg", 0, rows * ImGui.getTextLineHeightWithSpacing() + 6, true);
            for (int i = 0; i < currentSuggestions.size(); i++) {
                String s = currentSuggestions.get(i);
                if (ImGui.selectable(s + "##csugg_" + i)) {
                    applySuggestion(s);
                    reclaimFocus = true;
                }
            }
            if (suggestionsTruncated) {
                ImGui.textDisabled("... more (keep typing to narrow)");
            }
            ImGui.endChild();
        }

        ImGui.separator();
        ImGui.pushItemWidth(-60);
        boolean enterPressed = ImGui.inputText("##cinput", inputBuf,
                ImGuiInputTextFlags.EnterReturnsTrue
                        | ImGuiInputTextFlags.CallbackCompletion
                        | ImGuiInputTextFlags.CallbackHistory,
                new ImGuiInputTextCallback() {
                    @Override
                    public void accept(ImGuiInputTextCallbackData data) {
                        handleCallback(data);
                    }
                });
        ImGui.popItemWidth();

        if (reclaimFocus) {
            ImGui.setKeyboardFocusHere(-1);
            reclaimFocus = false;
        }

        ImGui.sameLine();
        boolean runPressed = ImGui.button("Run##crun");

        refreshSuggestions(inputBuf.get());

        if (enterPressed || runPressed) {
            String cmd = inputBuf.get().trim();
            if (!cmd.isEmpty()) {
                print("> " + cmd);
                if (cmdHistory.isEmpty() || !cmdHistory.getLast().equals(cmd)) {
                    cmdHistory.add(cmd);
                }
                historyIdx = -1;
                CommandManager.execute(cmd);
                inputBuf.set("");
                currentSuggestions = List.of();
                lastSuggestionInput = "";
                reclaimFocus = true;
            }
        }

        ImGui.end();
        if (!openBool.get()) visible = false;
    }

    private void handleCallback(ImGuiInputTextCallbackData data) {
        int flag = data.getEventFlag();
        if (flag == ImGuiInputTextFlags.CallbackCompletion) {
            onCompletion(data);
        } else if (flag == ImGuiInputTextFlags.CallbackHistory) {
            onHistory(data);
        }
    }

    private void onCompletion(ImGuiInputTextCallbackData data) {
        String current = inputBuf.get();
        List<String> suggs = getSuggestions(current);
        if (suggs.isEmpty()) return;

        String completed = completeWith(current, suggs.getFirst());
        if (completed == null) return;
        data.deleteChars(0, data.getBufTextLen());
        data.insertChars(0, completed);
    }

    /** Build the input string that results from accepting the given suggestion. */
    private String completeWith(String current, String suggestion) {
        String token = suggestion.split(" - ")[0].split("<")[0].trim();
        if (token.isEmpty()) return null;
        String[] parts = current.split(" +", -1);
        parts[parts.length - 1] = token;
        return String.join(" ", parts) + " ";
    }

    private void applySuggestion(String suggestion) {
        String completed = completeWith(inputBuf.get(), suggestion);
        if (completed == null) return;
        inputBuf.set(completed);
        refreshSuggestions(completed);
    }

    private void onHistory(ImGuiInputTextCallbackData data) {
        if (cmdHistory.isEmpty()) return;
        if (data.getEventKey() == ImGuiKey.UpArrow) {
            historyIdx = Math.min(historyIdx + 1, cmdHistory.size() - 1);
        } else if (data.getEventKey() == ImGuiKey.DownArrow) {
            historyIdx = Math.max(historyIdx - 1, -1);
        }
        String entry = historyIdx >= 0 ? cmdHistory.get(cmdHistory.size() - 1 - historyIdx) : "";
        data.deleteChars(0, data.getBufTextLen());
        if (!entry.isEmpty()) data.insertChars(0, entry);
    }

    private void refreshSuggestions(String input) {
        if (input.equals(lastSuggestionInput)) return;
        lastSuggestionInput = input;
        currentSuggestions = getSuggestions(input);
    }

    private List<String> getSuggestions(String cmd) {
        suggestionsTruncated = false;
        if (cmd.isEmpty()) return List.of();
        List<String> result = new ArrayList<>();

        String[] parts = cmd.split(" +", -1);
        String cmdName = parts[0].toLowerCase();
        String[] cmdArgs = Arrays.copyOfRange(parts, 1, parts.length);

        if (cmd.endsWith(" ")) {
            cmdArgs = Arrays.copyOf(cmdArgs, cmdArgs.length + 1);
            cmdArgs[cmdArgs.length - 1] = "";
        }

        if (cmdArgs.length > 0) {
            Command c = CommandManager.getCommandByAlias(cmdName);
            if (c == null) return result;
            String[] raw = c.getSuggestionsWithType(cmdArgs.length - 1, cmdArgs).suggestionSupplier().get();
            if (raw == null) return result;
            String last = cmdArgs[cmdArgs.length - 1];
            for (String s : raw) {
                if (last.isEmpty() || s.startsWith("<") || s.startsWith(last)) {
                    if (result.size() >= MAX_SUGGESTIONS) { suggestionsTruncated = true; break; }
                    result.add(s);
                }
            }
            return result;
        }

        for (Command c : CommandManager.getCommands()) {
            for (String alias : c.getAliases()) {
                if (alias.startsWith(cmdName)) {
                    if (result.size() >= MAX_SUGGESTIONS) { suggestionsTruncated = true; break; }
                    result.add(alias + " - " + c.getDescription());
                    break;
                }
            }
            if (suggestionsTruncated) break;
        }
        return result;
    }
}
