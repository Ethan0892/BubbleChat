package dev.bubblecraft.bubblechat.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerData {
    private ChatChannel channel = ChatChannel.GLOBAL;
    private long mutedUntilEpochMs = 0L;
    private String muteReason = null;

    private boolean shoutMode = false;

    private final Set<UUID> ignored = new HashSet<>();

    private UUID lastReplyTarget = null;
    private boolean socialSpy = false;

    private boolean commandSpy = false;

    private boolean announcementsOptOut = false;

    private boolean hideJoinMessages = false;
    private boolean hideQuitMessages = false;
    private boolean hideDeathMessages = false;

    private boolean blockPrivateMessages = false;

    private long lastChatAtEpochMs = 0L;
    private String lastChatNormalized = null;
    private int duplicateCount = 0;

    public ChatChannel getChannel() {
        return channel;
    }

    public void setChannel(ChatChannel channel) {
        if (channel != null) this.channel = channel;
    }

    public long getMutedUntilEpochMs() {
        return mutedUntilEpochMs;
    }

    public void setMutedUntilEpochMs(long mutedUntilEpochMs) {
        this.mutedUntilEpochMs = mutedUntilEpochMs;
    }

    public String getMuteReason() {
        return muteReason;
    }

    public void setMuteReason(String muteReason) {
        this.muteReason = muteReason;
    }

    public boolean isShoutMode() {
        return shoutMode;
    }

    public void setShoutMode(boolean shoutMode) {
        this.shoutMode = shoutMode;
    }

    public boolean isMutedNow(long nowEpochMs) {
        return mutedUntilEpochMs > nowEpochMs;
    }

    public Set<UUID> getIgnored() {
        return ignored;
    }

    public UUID getLastReplyTarget() {
        return lastReplyTarget;
    }

    public void setLastReplyTarget(UUID lastReplyTarget) {
        this.lastReplyTarget = lastReplyTarget;
    }

    public boolean isSocialSpy() {
        return socialSpy;
    }

    public void setSocialSpy(boolean socialSpy) {
        this.socialSpy = socialSpy;
    }

    public boolean isCommandSpy() {
        return commandSpy;
    }

    public void setCommandSpy(boolean commandSpy) {
        this.commandSpy = commandSpy;
    }

    public boolean isAnnouncementsOptOut() {
        return announcementsOptOut;
    }

    public void setAnnouncementsOptOut(boolean announcementsOptOut) {
        this.announcementsOptOut = announcementsOptOut;
    }

    public boolean isHideJoinMessages() {
        return hideJoinMessages;
    }

    public void setHideJoinMessages(boolean hideJoinMessages) {
        this.hideJoinMessages = hideJoinMessages;
    }

    public boolean isHideQuitMessages() {
        return hideQuitMessages;
    }

    public void setHideQuitMessages(boolean hideQuitMessages) {
        this.hideQuitMessages = hideQuitMessages;
    }

    public boolean isHideDeathMessages() {
        return hideDeathMessages;
    }

    public void setHideDeathMessages(boolean hideDeathMessages) {
        this.hideDeathMessages = hideDeathMessages;
    }

    public boolean isBlockPrivateMessages() {
        return blockPrivateMessages;
    }

    public void setBlockPrivateMessages(boolean blockPrivateMessages) {
        this.blockPrivateMessages = blockPrivateMessages;
    }

    public long getLastChatAtEpochMs() {
        return lastChatAtEpochMs;
    }

    public void setLastChatAtEpochMs(long lastChatAtEpochMs) {
        this.lastChatAtEpochMs = lastChatAtEpochMs;
    }

    public String getLastChatNormalized() {
        return lastChatNormalized;
    }

    public void setLastChatNormalized(String lastChatNormalized) {
        this.lastChatNormalized = lastChatNormalized;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }
}
