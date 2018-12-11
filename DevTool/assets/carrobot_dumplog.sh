LOG_DIR=/storage/sdcard0/ileja_logs/
LOG_DIR_BK=/storage/sdcard0/ileja_logs_bk/
LogFile=/storage/sdcard0/dump_log_info.log
DUMP_LOG_SCRIPT_FILE_EXT=/storage/sdcard1/carrobot_dumplog.sh
PREDEFINE_SCRIPT_FILE=/storage/sdcard1/carrobot.sh
CMD_TYPE_PROP=root.cmd.type
CMD_RUN_PROP=root.cmd.remote
CMD_TYPE_VALUE="aios"

#output redirect path: /storage/sdcard0/dump_log_info.log
exec 0>&1
exec 2>&1
exec 1>>$LogFile

echo "==================================="
echo "============DUMP HEADER============"
echo "DUMP EXEC time : "
date
echo "===========END DUMP HEADER========="
echo "==================================="

get_DeveloperMode() {
    if [ -f "${DUMP_LOG_SCRIPT_FILE_EXT}" ] || [ -f "${PREDEFINE_SCRIPT_FILE}" ]; then
        echo 1
    else
        echo 0
    fi
}

IS_DEVELOP_MODE="$(get_DeveloperMode)"

get_datetime() {
	DATETIME=$(date "+%Y.%m.%d_%H.%M.%S")
	echo $DATETIME
}

LOG_FILENAME="$(get_datetime)"

LOG_PATH="${LOG_DIR}""${LOG_FILENAME}""/"

if [ ! -d "${LOG_DIR}" ]; then
    mkdir "${LOG_DIR}"
    echo "mkdir " "${LOG_DIR}"
fi

if [ ! -d "${LOG_PATH}" ]; then
    mkdir "${LOG_PATH}"
	echo "mkdir " "${LOG_PATH}"
fi

echo "inited==================="

# trigger to catch aios log and upload firstly
setprop $CMD_TYPE_PROP "$CMD_TYPE_VALUE"
setprop $CMD_RUN_PROP 1
sleep 5

dumpsys cpuinfo  > "${LOG_PATH}""cpuinfo.log"
ps -t -p -P > "${LOG_PATH}""ps.log"
logcat -b main -v threadtime -d -t 5000 -N 5000 -f "${LOG_PATH}""main.log"
logcat -b system -v threadtime -d -f "${LOG_PATH}""system.log"
logcat -b events -v threadtime -d -f "${LOG_PATH}""events.log"
logcat -b radio -v threadtime -d -f "${LOG_PATH}""radio.log"
dmesg > "${LOG_PATH}""dmesg.log"
dumpsys batterystats > "${LOG_PATH}""batterystats.log"
dumpsys connectivity > "${LOG_PATH}""connectivity.log"
getprop > "${LOG_PATH}""prop.log"

echo "sys base info==================="

cat /sys/bootinfo/powerup_reason > "${LOG_PATH}""powerup_reason"
cp /data/system/packages.xml "${LOG_PATH}"


echo "sys dropbox info==================="

DROPBOX_DIR=/data/system/dropbox/

echo `ls -l /data/system/dropbox| grep "system_app*"` > "${DROPBOX_DIR}""dropbox.txt"
echo "===================\n" >> "${DROPBOX_DIR}""dropbox.txt"
for tmp in `ls /data/system/dropbox | grep "system_app*"`; do
	echo "-------$tmp-------" >> "${DROPBOX_DIR}""dropbox.txt"
	cat "${DROPBOX_DIR}""$tmp" >> "${DROPBOX_DIR}""dropbox.txt"
done
mv "${DROPBOX_DIR}""dropbox.txt" "${LOG_PATH}""dropbox.txt"
#删除内容，不删目录
rm -rf /data/system/dropbox/*


echo "sys anr info==================="

ANR_DIR=/data/anr/

echo `ls -l /data/anr | grep "traces*"` > "${ANR_DIR}""anr.txt"
echo "===================\n" >> "${ANR_DIR}""anr.txt"
for tmp in `ls /data/anr | grep "traces*"`; do
	echo "-------$tmp-------" >> "${ANR_DIR}""anr.txt"
	cat "${ANR_DIR}""$tmp" >> "${ANR_DIR}""anr.txt"
done
mv "${ANR_DIR}""anr.txt" "${LOG_PATH}""anr.txt"
#删除内容，不删目录
rm -rf /data/anr/*

cp -r /sdcard/Android/data/com.aispeech.aios/cache/AIOSLog.txt "${LOG_PATH}""AIOS_AILog.txt"
cp -r /sdcard/Android/data/com.aispeech.aios/cache/AIOSEngineLog.txt "${LOG_PATH}""AIOSEngine_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.bluetoothext/cache/AIBTLog.txt "${LOG_PATH}""AIBT_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.carrobot/cache/AILog.txt "${LOG_PATH}""Launcher_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.carrobot.aichat/cache/AILog.txt "${LOG_PATH}""Chat_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.carrobot.mapdownloader/cache/AILog.txt "${LOG_PATH}""MapDown_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.carrobot.music/cache/AILog.txt "${LOG_PATH}""FM_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.carrobot.navigation/cache/AILog.txt "${LOG_PATH}""Navi_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.carrobot.phone/cache/AILog.txt "${LOG_PATH}""Phone_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.carrobot.traffic/cache/AILog.txt "${LOG_PATH}""Traffic_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.carrobot.wechat/cache/AILog.txt "${LOG_PATH}""WeChat_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.ailbs/cache/AILog.txt "${LOG_PATH}""AILBS_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.gesture/cache/AILog.txt "${LOG_PATH}""Gesture_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.aicar/cache/AILog.txt "${LOG_PATH}""AICar_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.aicore/cache/AILog.txt "${LOG_PATH}""AICore_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.aitelcomm/cache/AILog.txt "${LOG_PATH}""AITelcomm_AILog.txt"
cp -r /sdcard/Android/data/com.ileja.fotaupgrade/cache/fotaupgrade.log "${LOG_PATH}""FotaUpgrade_AILog.txt"
cp -r /data/carrobot/obdvolt.log "${LOG_PATH}""obdvolt.log"

echo "cp app log info==================="
#cp -r /data/system/dropbox "${LOG_PATH}"
#cp -r /data/tombstones "${LOG_PATH}"
#cp -r /sdcard/mtklog/audio_dump "${LOG_PATH}""audio_dump"
#cp -r /sdcard/mtklog/aee_exp "${LOG_PATH}""aee_exp1"
#rm -rf /sdcard/mtklog/aee_exp/*
#cp -r /data/aee_exp "${LOG_PATH}""aee_exp2"

echo "cp bt hci_log and hfp_audio_log==================="
cp -r /sdcard/btsnoop_hci.log "${LOG_PATH}""hci_dump.log"
cp -r /sdcard/mtklog/audio_dump "${LOG_PATH}""audio_dump"

sleep 2
cp -r /sdcard/Pictures/Screenshots "${LOG_PATH}"
rm /sdcard/Pictures/Screenshots/*

echo "cp Screenshots info==================="

rm -rf /sdcard/Android/data/com.aispeech.aios/cache/AIOSLog.txt
rm -rf /sdcard/Android/data/com.aispeech.aios/cache/AIOSEngineLog.txt
rm -rf /sdcard/Android/data/com.ileja.bluetoothext/cache/AIBTLog.txt
rm -rf /sdcard/Android/data/com.ileja.carrobot/cache/AILog.txt
rm -rf /sdcard/Android/data/com.ileja.carrobot.aichat/cache/AILog.txt
rm -rf /sdcard/Android/data/com.ileja.carrobot.mapdownloader/cache/AILog.txt
rm -rf /sdcard/Android/data/com.ileja.carrobot.music/cache/AILog.txt
rm -rf /sdcard/Android/data/com.ileja.carrobot.navigation/cache/AILog.txt
rm -rf /sdcard/Android/data/com.ileja.carrobot.phone/cache/AILog.txt
rm -rf /sdcard/Android/data/com.ileja.carrobot.traffic/cache/AILog.txt
rm -rf /sdcard/Android/data/com.ileja.carrobot.wechat/cache/AILog.txt
rm -rf /sdcard/Android/data/com.ileja.ailbs/cache/AILog.txt
rm -rf /sdcard/Android/data/com.ileja.gesture/cache/AILog.txt
rm -rf /sdcard/Android/data/com.ileja.aicar/cache/AILog.txt
rm -rf /sdcard/Android/data/com.ileja.aicore/cache/AILog.txt
rm -rf /sdcard/Android/data/com.ileja.aitelcomm/cache/AILog.txt
rm -rf /sdcard/Android/data/com.ileja.fotaupgrade/cache/fotaupgrade.log

echo "rm app log info==================="

#单个文件40000行压缩后50K左右
MIN_LINE=5000
MAX_LINE=40000
tail -n ${MAX_LINE} "${LOG_PATH}""AIOS_AILog.txt" > "${LOG_PATH}""AIOS.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""AIOSEngine_AILog.txt"  > "${LOG_PATH}""AIOSEngine.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""AIBT_AILog.txt"  > "${LOG_PATH}""AIBT.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""Launcher_AILog.txt"  > "${LOG_PATH}""Launcher.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""Chat_AILog.txt"  > "${LOG_PATH}""Chat.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""MapDown_AILog.txt"  > "${LOG_PATH}""MapDown.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""FM_AILog.txt"  > "${LOG_PATH}""FM.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""Navi_AILog.txt"  > "${LOG_PATH}""Navi.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""Phone_AILog.txt"  > "${LOG_PATH}""Phone.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""Traffic_AILog.txt"  > "${LOG_PATH}""Traffic.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""WeChat_AILog.txt"  > "${LOG_PATH}""WeChat.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""AILBS_AILog.txt"  > "${LOG_PATH}""AILBS.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""Gesture_AILog.txt"  > "${LOG_PATH}""Gesture.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""AICar_AILog.txt"  > "${LOG_PATH}""AICar.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""AICore_AILog.txt"  > "${LOG_PATH}""AICore.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""AITelcomm_AILog.txt"  > "${LOG_PATH}""AITelcomm.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""FotaUpgrade_AILog.txt"  > "${LOG_PATH}""FotaUpgrade.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""dropbox.txt"  > "${LOG_PATH}""system_dropbox.txt"
tail -n ${MAX_LINE} "${LOG_PATH}""anr.txt"  > "${LOG_PATH}""system_anr_trace.txt"

echo "tail app log info==================="

rm "${LOG_PATH}""AIOS_AILog.txt"
rm "${LOG_PATH}""AIOSEngine_AILog.txt"
rm "${LOG_PATH}""AIBT_AILog.txt"
rm "${LOG_PATH}""Launcher_AILog.txt" 
rm "${LOG_PATH}""Chat_AILog.txt"
rm "${LOG_PATH}""MapDown_AILog.txt" 
rm "${LOG_PATH}""FM_AILog.txt"
rm "${LOG_PATH}""Navi_AILog.txt" 
rm "${LOG_PATH}""Phone_AILog.txt"
rm "${LOG_PATH}""Traffic_AILog.txt" 
rm "${LOG_PATH}""WeChat_AILog.txt"
rm "${LOG_PATH}""AILBS_AILog.txt"
rm "${LOG_PATH}""Gesture_AILog.txt"
rm "${LOG_PATH}""AICar_AILog.txt" 
rm "${LOG_PATH}""AICore_AILog.txt"
rm "${LOG_PATH}""AITelcomm_AILog.txt"
rm "${LOG_PATH}""FotaUpgrade_AILog.txt"
rm "${LOG_PATH}""dropbox.txt"
rm "${LOG_PATH}""anr.txt"

echo "rm old app log info==================="

cd "${LOG_DIR}"
tar -cvzf "${LOG_FILENAME}"".tar.gz" "${LOG_FILENAME}"

echo "tar==================="

# backup logs into sdcard when to be under develop mode
backupLogs() {
if [ ! -d "${LOG_DIR_BK}" ]; then
    mkdir "${LOG_DIR_BK}"
    echo "mkdir " "${LOG_DIR_BK}"
fi

if [ $IS_DEVELOP_MODE = "1" ]; then
    cp -r "$LOG_DIR." "$LOG_DIR_BK"
fi
}

rm -rf "${LOG_PATH}"
echo "rm -rf ${LOG_PATH}"

backupLogs

echo "rm log folder==================="
