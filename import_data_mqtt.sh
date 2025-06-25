#!/bin/bash

# Version info
echo """
=====================================================
Export 2 MQTT v1.0 (import_data_mqtt.sh)
=====================================================

Check prerequisites:
"""

# Setting variables
timenow() { date +"%d.%m.%Y %H:%M:%S"; }
path=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
while getopts ":l" opt; do
	case ${opt} in
		l) loop_switch="on" ;;
		\?) echo "$(timenow) MAIN * Invalid parameter: use -l for loop mode" >&2 ;;
	esac
done

# Prerequisites check
if [[ -f $path/user/export2mqtt.cfg ]] ; then
	echo "$(timenow) MAIN * export2mqtt.cfg file exists"
	source <(grep switch_ $path/user/export2mqtt.cfg)
	temp_log=${switch_temp_path}/temp.log
else echo "$(timenow) MAIN * Missing export2mqtt.cfg file, exit"
	exit 1
fi

# Check for required Python packages
echo "$(timenow) MAIN * Checking Python packages"
python3 -c "import paho.mqtt.client" 2>/dev/null || {
    echo "$(timenow) MAIN * Missing paho-mqtt package. Install with: pip3 install paho-mqtt"
    exit 1
}

# Loop function
loop_count=0
while true ; do
	[[ $loop_switch == "on" ]] && loop_count=$(($loop_count + 1))

	# BLE adapter status (hci0)
	if [[ $switch_miscale == "on" ]] || [[ $switch_s400 == "on" ]] || [[ $switch_omron == "on" ]] ; then
		ble_adapter_check=$switch_ble_adapter_check
		
		# BLE adapter search by MAC address
		if [[ $switch_ble_adapter_switch == "on" ]] ; then
			source <(grep ble_adapter_mac $path/user/export2mqtt.cfg)
			ble_status_all=$(hciconfig -a)
			ble_check=$(echo $ble_status_all | grep $ble_adapter_mac -B20 | grep hci. -o | tail -n 1)
			[[ -z $ble_check ]] && echo "$(timenow) BLE * MAC address not found, exit" && exit 1
			
		# BLE adapter search by HCI number
		else source <(grep ble_adapter_hci $path/user/export2mqtt.cfg)
			ble_check="hci$ble_adapter_hci"
		fi
		
		# Restarting an adapter, setting a random MAC
		ble_status_check=$(hciconfig $ble_check | grep -E "RUNNING|UP" | wc -l)
		ble_mac_random=$(hexdump -n 3 -ve '1/1 "%.2x "' /dev/urandom | awk '{print "00:16:3e:"$1":"$2":"$3}')
		if [[ $ble_status_check == "2" ]] ; then
			ble_status="ok"
			echo "$(timenow) BLE * $ble_check adapter is up & running"
		else ble_status="restart"
			echo "$(timenow) BLE * $ble_check adapter is down, activating"
			while [[ $ble_adapter_check == "on" ]] && [[ $ble_status == "restart" ]] ; do
				sudo hciconfig $ble_check down > /dev/null 2>&1
				sleep 2
				sudo hciconfig $ble_check up > /dev/null 2>&1
				sleep 2
				ble_status_check=$(hciconfig $ble_check | grep -E "RUNNING|UP" | wc -l)
				if [[ $ble_status_check == "2" ]] ; then
					ble_status="ok"
					echo "$(timenow) BLE * $ble_check adapter is up & running"
				else sleep 10
				fi
			done
		fi
		echo "$(timenow) BLE * Changing $ble_check adapter MAC to $ble_mac_random"
		sudo hciconfig $ble_check down > /dev/null 2>&1
		sleep 2
		sudo $path/user/mac_spoof $ble_check $ble_mac_random > /dev/null 2>&1
		sleep 2
		sudo hciconfig $ble_check up > /dev/null 2>&1
		sleep 2
	fi

	# Mi Body Composition Scale 2
	if [[ $switch_miscale == "on" ]] ; then
		miscale_backup=$path/user/miscale_backup.csv
		echo "$(timenow) MISCALE * Module is ON in export2mqtt.cfg file"
		
		# Creating $miscale_backup file
		if [[ ! -f $miscale_backup ]] ; then
			miscale_header="Data Status;Unix Time;Date [dd.mm.yyyy];Time [hh:mm];Weight [kg];Change [kg];BMI;Body Fat [%];Skeletal Muscle Mass [kg];Bone Mass [kg];Body Water [%];Physique Rating;Visceral Fat;Metabolic Age [years];BMR [kCal];LBM [kg];Ideal Wieght [kg];Fat Mass To Ideal [type:mass kg];Protein [%];Impedance;Email User;Upload Date [dd.mm.yyyy];Upload Time [hh:mm];Difference Time [s]"
			[[ $switch_mqtt == "on" ]] && miscale_header="$miscale_header;Battery [V];Battery [%]"
			echo "$(timenow) MISCALE * Creating miscale_backup.csv file, checking for new data"
			echo $miscale_header > $miscale_backup
		else echo "$(timenow) MISCALE * miscale_backup.csv file exists, checking for new data"
		fi
		
		# Importing raw data from source (BLE or MQTT)
		if [[ $switch_mqtt == "on" ]] ; then
			source <(grep miscale_mqtt_ $path/user/export2mqtt.cfg)
			echo "$(timenow) MISCALE * Importing data from an MQTT broker"
			miscale_read=$(mosquitto_sub -h localhost -t 'data' -u "$miscale_mqtt_user" -P "$miscale_mqtt_passwd" -C 1 -W 10)
			miscale_unixtime=$(echo $miscale_read | cut -d ";" -f 1)
			if [[ -z $miscale_unixtime ]] ; then
				echo "$(timenow) MISCALE * No MQTT data, check connection to MQTT broker or ESP32"
			fi
		elif [[ $ble_status == "ok" ]] ; then
			echo "$(timenow) MISCALE * Importing data from a BLE adapter"
			if echo $ble_check | grep -q "incomplete" ; then
				echo "$(timenow) MISCALE * Reading BLE data incomplete, repeat weighing"
			else miscale_read=$(echo $ble_check | awk '{sub(/.*BLE scan/, ""); print substr($1,1)}')
				miscale_unixtime=$(echo $miscale_read | cut -d ";" -f 1)
			fi
		fi
		
		# Check time synchronization between scale and OS
		if [[ -n $miscale_unixtime ]] ; then
			source <(grep miscale_time_ $path/user/export2mqtt.cfg)
			miscale_os_unixtime=$(date +%s)
			miscale_time_zone=$(date +%z | cut -c1-3)
			miscale_offset_unixtime=$(( $miscale_unixtime + $miscale_time_zone * 3600 + $miscale_time_offset ))
			miscale_time_shift=$(( $miscale_os_unixtime - $miscale_offset_unixtime ))
			miscale_absolute_shift=${miscale_time_shift#-}
			if (( $miscale_absolute_shift < $miscale_time_unsync )) ; then
				miscale_found_entry=false
				
				# Check for duplicates, similar raw data in $miscale_backup file
				while IFS=";" read -r _ unix_time _ ; do
					if [[ $unix_time =~ ^[0-9]+$ ]] ; then
						miscale_time_dif=$(($miscale_offset_unixtime - $unix_time))
						miscale_time_dif=${miscale_time_dif#-}
						if (( $miscale_time_dif < $miscale_time_check )) ; then
							miscale_found_entry=true
							break
						fi
					fi
				done < $miscale_backup
				
				# Save raw data to $miscale_backup file
				if [[ $miscale_found_entry == "false" ]] ; then
					echo "$(timenow) MISCALE * Saving import $miscale_offset_unixtime to miscale_backup.csv file"
					miscale_offset_row=${miscale_read/${miscale_unixtime}/to_import;${miscale_offset_unixtime}}
					echo $miscale_offset_row >> $miscale_backup
				else echo "$(timenow) MISCALE * $miscale_time_dif s time difference, same or similar data already exists in miscale_backup.csv file"
				fi
			else echo "$(timenow) MISCALE * $miscale_time_shift s time difference, synchronize date and time scale"
				echo "$(timenow) MISCALE * Time offset is set to $miscale_offset s"
			fi
		fi
		
		# Calculating data and upload to MQTT, print to temp.log file
		if grep -q "failed\|to_import" $miscale_backup ; then
			python3 -B $path/miscale/miscale_export_mqtt.py > $temp_log 2>&1
			miscale_import=$(awk -F ": " '/MISCALE /*/ Import data:/{print substr($2,1,10)}' $temp_log)
			echo "$(timenow) MISCALE * Calculating data from import $miscale_import, upload to MQTT broker"
		fi
		
		# Handling errors from temp.log file
		if [[ -z $miscale_import ]] ; then
			echo "$(timenow) MISCALE * There is no new data to upload to MQTT broker"
		elif grep -q "MISCALE \* There" $temp_log ; then
			echo "$(timenow) MISCALE * There is no user with given weight or undefined user email@email.com, check users section in export2mqtt.cfg"
			echo "$(timenow) MISCALE * Deleting import $miscale_import from miscale_backup.csv file"
			sed -i "/$miscale_import/d" $miscale_backup
		elif grep -q "Err\|Exception\|Failed" $temp_log ; then
			echo "$(timenow) MISCALE * Upload to MQTT broker has failed, check temp.log for error details"
			sed -i "s/to_import;$miscale_import/failed;$miscale_import/" $miscale_backup
		else echo "$(timenow) MISCALE * Data upload to MQTT broker is complete"
			
			# Save calculated data to $miscale_backup file
			echo "$(timenow) MISCALE * Saving calculated data from import $miscale_import to miscale_backup.csv file"
			miscale_import_data=$(awk -F ": " '/MISCALE /*/ Import data:/{print $2}' $temp_log)
			miscale_calc_data=$(awk -F ": " '/MISCALE /*/ Calculated data:/{print $2}' $temp_log)
			miscale_import_diff=$(echo $miscale_calc_data | cut -d ";" -f 1-3)
			miscale_check_line=$(wc -l < $miscale_backup)
			sed -i "s/failed;$miscale_import_data/uploaded;$miscale_import;$miscale_calc_data;$miscale_time_shift/; s/to_import;$miscale_import_data/uploaded;$miscale_import;$miscale_calc_data;$miscale_time_shift/" $miscale_backup
			if [[ $miscale_check_line == "2" ]] ; then
				sed -i "s/$miscale_import;$miscale_import_diff/$miscale_import;$miscale_import_diff;0.0/" $miscale_backup
			else miscale_email_user=$(echo $miscale_calc_data | cut -d ";" -f 18)
				miscale_weight_last=$(grep $miscale_email_user $miscale_backup | sed -n 'x;$p' | cut -d ";" -f 5)
				miscale_weight_import=$(echo $miscale_calc_data | cut -d ";" -f 3)
				miscale_weight_diff=$(echo $miscale_weight_import - $miscale_weight_last | bc | sed "s/^-\./-0./; s/^\./0./")
				sed -i "s/$miscale_import;$miscale_import_diff/$miscale_import;$miscale_import_diff;$miscale_weight_diff/; s/;0;/;0.0;/" $miscale_backup
			fi
		fi
		unset $(compgen -v | grep '^miscale_')
	else echo "$(timenow) MISCALE * Module is OFF in export2mqtt.cfg file"
	fi

	# Omron blood pressure
	if [[ $switch_omron == "on" ]] ; then
		omron_backup=$path/user/omron_backup.csv
		echo "$(timenow) OMRON * Module is ON in export2mqtt.cfg file"
		
		# Creating $omron_backup file
		if [[ ! -f $omron_backup ]] ; then
			echo "Data Status;Unix Time;Date [dd.mm.yyyy];Time [hh:mm];SYStolic [mmHg];DIAstolic [mmHg];Heart Rate [bpm];Category;MOV;IHB;Email User;Upload Date [dd.mm.yyyy];Upload Time [hh:mm];Difference Time [s]" > $omron_backup
			echo "$(timenow) OMRON * Creating $omron_backup file, checking for new data"
		else echo "$(timenow) OMRON * omron_backup.csv file exists, checking for new data"
		fi
		
		# Importing raw data from source (BLE)
		if [[ $ble_status == "ok" ]] ; then
			echo "$(timenow) OMRON * Importing data from a BLE adapter"
			coproc ble { bluetoothctl; }
			while true ; do
				source <(grep omron_omblepy_ $path/user/export2mqtt.cfg)
				omron_hci=$(echo $ble_check | grep -o 'hci.' | head -n 1)
				omron_omblepy_check=$(timeout ${omron_omblepy_time}s python3 -B $path/omron/omblepy.py -a $omron_hci -p -d $omron_omblepy_model 2> /dev/null)
				if echo $omron_omblepy_check | grep -q $omron_omblepy_mac ; then
					
					# Adding an exception for selected models
					if [[ $omron_omblepy_model == "hem-6232t" ]] || [[ $omron_omblepy_model == "hem-7530t" ]] ; then
						omron_omblepy_flags="-n"
					else omron_omblepy_flags="-n -t"
					fi
					if [[ $omron_omblepy_debug == "on" ]] ; then
						python3 -B $path/omron/omblepy.py -a $omron_hci -d $omron_omblepy_model --loggerDebug -m $omron_omblepy_mac
					elif [[ $omron_omblepy_all == "on" ]] ; then
						python3 -B $path/omron/omblepy.py -a $omron_hci -d $omron_omblepy_model -m $omron_omblepy_mac >/dev/null 2>&1
					else python3 -B $path/omron/omblepy.py $omron_omblepy_flags -a $omron_hci -d $omron_omblepy_model -m $omron_omblepy_mac >/dev/null 2>&1
					fi
				else exec {ble[0]}>&-
					exec {ble[1]}>&-
					wait $ble_PID
					break
				fi
			done
			if [[ -f "$switch_temp_path/omron_user1.csv" ]] || [[ -f "$switch_temp_path/omron_user2.csv" ]] ; then
				source <(grep omron_export_user $path/user/export2mqtt.cfg)
				echo "$(timenow) OMRON * Prepare data for omron_backup.csv file"
				awk -F ';' 'NR==FNR{a[$2];next}!($2 in a)' $omron_backup $switch_temp_path/omron_user1.csv > $switch_temp_path/omron_users.csv
				awk -F ';' 'NR==FNR{a[$2];next}!($2 in a)' $omron_backup $switch_temp_path/omron_user2.csv >> $switch_temp_path/omron_users.csv
				sed -i "s/ /;/g; s/user1/$omron_export_user1/; s/user2/$omron_export_user2/" $switch_temp_path/omron_users.csv
				grep -q "email@email.com" $switch_temp_path/omron_users.csv && echo "$(timenow) OMRON * Deleting records with undefined user email@email.com, check users section in export2mqtt.cfg file" && sed -i "/email@email\.com/d" $switch_temp_path/omron_users.csv
				cat $switch_temp_path/omron_users.csv >> $omron_backup
				rm $switch_temp_path/omron_user*.csv
			fi
		fi
		
		# Upload to MQTT broker, print to temp.log file
		if grep -q "failed\|to_import" $omron_backup ; then
			if [[ $switch_miscale == "on" ]] ; then
				python3 -B $path/omron/omron_export_mqtt.py >> $temp_log 2>&1
			else python3 -B $path/omron/omron_export_mqtt.py > $temp_log 2>&1
			fi
			omron_import=$(awk -F ": " '/OMRON /*/ Import data:/{print substr($2,1,10)}' $temp_log)
			echo "$(timenow) OMRON * Calculating data from import $omron_import, upload to MQTT broker"
		fi
		
		# Handling errors, save data to $omron_backup file
		if [[ -z $omron_import ]] ; then
			echo "$(timenow) OMRON * There is no new data to upload to MQTT broker"
		else omron_import_data=$(awk -F ": " '/OMRON /*/ Import data:/{print $2}' $temp_log)
			omron_cut_data=$(echo $omron_import_data | cut -d ";" -f 1-6)
			omron_calc_data=$(awk -F ": " '/OMRON /*/ Calculated data:/{print $2}' $temp_log)
			omron_os_unixtime=$(date +%s)
			omron_time_shift=$(( $omron_os_unixtime - $omron_import ))
			if grep -q "Err\|Exception\|Failed" $temp_log ; then
				if grep -q "MISCALE \* Upload" $temp_log ; then
					echo "$(timenow) OMRON * Upload to MQTT broker has failed, check temp.log file for error details"
					sed -i "s/to_import;$omron_import/failed;$omron_import/" $omron_backup
				elif grep -q "OMRON \* Upload" $temp_log ; then
					echo "$(timenow) OMRON * Data upload to MQTT broker is complete"
					echo "$(timenow) OMRON * Saving calculated data from import $omron_import to omron_backup.csv file"
					sed -i "s/failed;$omron_import_data/uploaded;omron_cut_data;$omron_calc_data;$omron_time_shift/; s/to_import;$omron_import_data/uploaded;$omron_cut_data;$omron_calc_data;$omron_time_shift/" $omron_backup
				else echo "$(timenow) OMRON * Upload to MQTT broker has failed, check temp.log file for error details"
					sed -i "s/to_import;$omron_import/failed;$omron_import/" $omron_backup
				fi
			else echo "$(timenow) OMRON * Data upload to MQTT broker is complete"
				echo "$(timenow) OMRON * Saving calculated data from import $omron_import to omron_backup.csv file"
				sed -i "s/failed;$omron_import_data/uploaded;$omron_cut_data;$omron_calc_data;$omron_time_shift/; s/to_import;$omron_import_data/uploaded;$omron_cut_data;$omron_calc_data;$omron_time_shift/" $omron_backup
			fi
		fi
		unset $(compgen -v | grep '^omron_')
	else echo "$(timenow) OMRON * Module is OFF in export2mqtt.cfg file"
	fi
	[[ $loop_count -eq 1 ]] && break
done