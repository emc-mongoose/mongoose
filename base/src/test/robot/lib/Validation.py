import csv

def validate_item_output_file(
	item_output_file_name, item_output_path, item_size_min=0, item_size_max=9000000000000000000
):
	with open(item_output_file_name, "rb") as item_output_file:
		reader = csv.reader(item_output_file)
		for row in reader:
			actual_item_output_path, actual_item_name = row[0].split('/', 1)
			assert item_output_path == actual_item_output_path, \
				"Item output path: '{}', expected: '{}".format(actual_item_output_path, item_output_path)
			item_size = int(row[2])
			assert item_size >= item_size_min, "Item size: {}, expected >= {}".format(item_size, item_size_min)
			assert item_size <= item_size_max, "Item size: {}, expected <= {}".format(item_size, item_size_max)

def validate_log_file_metrics_total(
	log_dir, file_separator='/', op_type="CREATE", count_succ_min=0, count_succ_max=9000000000000000000, count_fail_max=0,
	transfer_size=0, transfer_size_delta=0
):
	log_file_name = file_separator.join((log_dir, "metrics.total.csv"))
	with open(log_file_name, "r") as metrics_total_file:
		reader = csv.DictReader(metrics_total_file)
		row_found = False
		for row in reader:
			if op_type == row["OpType"]:
				actual_count_succ = int(row["CountSucc"])
				assert actual_count_succ <= count_succ_max, \
					"Successful op count: {}, expected <= {}".format(actual_count_succ, count_succ_max)
				assert actual_count_succ >= count_succ_min, \
					"Successful op count: {}, expected >= {}".format(actual_count_succ, count_succ_min)
				actual_count_fail = int(row["CountFail"])
				assert actual_count_fail <= count_fail_max, \
					"Failed op count: {}, expected <= {}".format(actual_count_fail, count_fail_max)
				actual_transfer_size = int(row["Size"])
				assert actual_transfer_size <= transfer_size + transfer_size_delta, \
					"Transfer size: {}, expected <= {}".format(actual_transfer_size, transfer_size + transfer_size_delta)
				assert actual_transfer_size >= transfer_size - transfer_size_delta, \
					"Transfer size: {}, expected >= {}".format(actual_transfer_size, transfer_size - transfer_size_delta)
				row_found = True
				break
		assert row_found, "%s: row containing the record w/ op type '%s' was not found".format(log_file_name, op_type)
