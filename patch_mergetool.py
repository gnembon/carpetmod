#!/usr/bin/python3

# This interactive script resolves merge conflicts in the patch files
# patch_mergetool.py <patchFiles...>

import os
import re
import sys

# Classes

# Fields:
#  header
#  hunks
class PatchFile:
	pass

# Fields:
#  start_a
#  len_a
#  start_b
#  len_b
#  suffix
#  lines
#  is_duplicate (set later)
class Hunk:
	pass

# Fields:
#  content
#  added
class Line:
	def __init__(self, content, added):
		self.content = content
		self.added = added
	def __eq__(self, other):
		return self.content == other.content and self.added == other.added

# Matches the @@ hunk headers @@
hunk_header_pattern = re.compile(r"^@@ -(\d+),(\d+) \+(\d+),(\d+) @@(.*)$", flags = re.DOTALL)

# Separates a file with conflict markers into the two files that are trying to be merged
def get_each_content(content):
	content_a = []
	content_b = []
	in_a = False
	in_b = False
	for line in content.splitlines(keepends = True):
		if in_a:
			if line.startswith("======="):
				in_a = False
				in_b = True
			else:
				content_a.append(Line(line, True))
		elif in_b:
			if line.startswith(">>>>>>>"):
				in_b = False
			else:
				content_b.append(Line(line, True))
		else:
			if line.startswith("<<<<<<<"):
				in_a = True
			else:
				content_a.append(Line(line, False))
				content_b.append(Line(line, False))
	return (content_a, content_b)

# Parses the lines of a patch file into a patch file object
def parse_file(lines):
	patch = PatchFile()
	patch.hunks = []
	current_hunk = None
	current_lines = []
	for line in lines:
		match = hunk_header_pattern.match(line.content)
		if match != None:
			if current_hunk == None:
				patch.header = current_lines
			current_hunk = Hunk()
			current_hunk.start_a = int(match.group(1))
			current_hunk.len_a = int(match.group(2))
			current_hunk.start_b = int(match.group(3))
			current_hunk.len_b = int(match.group(4))
			current_hunk.suffix = match.group(5)
			current_lines = []
			current_hunk.lines = current_lines
			patch.hunks.append(current_hunk)
		else:
			current_lines.append(line)
	return patch

# Takes old file content, returns new content, unless merging failed, then it returns None
def process_file(content):
	# Split Content
	lines_a, lines_b = get_each_content(content)
	
	# Parse files
	file_a = parse_file(lines_a)
	file_b = parse_file(lines_b)
	
	# Header conflicts unresolvable
	if file_a.header != file_b.header:
		return None
	
	# Get list of all hunks, sorted in order of where the patches are applied in the original file
	sorted_hunks = file_a.hunks + file_b.hunks
	sorted_hunks.sort(key = lambda hunk: hunk.start_a)
	
	# Resolve conflicts (hunks applied in the same place which differ) and automatically resolve duplicates (hunks applied in the same place which don't differ)
	last_hunk = None
	conflict_count = 0
	for hunk in sorted_hunks:
		hunk.is_duplicate = False
		if last_hunk != None:
			if hunk.start_a < last_hunk.start_a + last_hunk.len_a: # if hunks overlap
				if hunk.start_a != last_hunk.start_a or hunk.len_a != last_hunk.len_a or hunk.len_b != last_hunk.len_b or hunk.lines != last_hunk.lines: # if hunks conflict
					conflict_count += 1
					print("Conflict #" + str(conflict_count) + ":")
					print("  Hunk A:")
					for line in last_hunk.lines:
						print("    " + line.content)
					print("  Hunk B:")
					for line in hunk.lines:
						print("    " + line.content)
					answer = input("  Which hunk should be preferred (A/B)? Or press enter to abort the merge: ").lower()
					if answer.startswith("a"):
						hunk.is_duplicate = True
					elif answer.startswith("b"):
						last_hunk.is_duplicate = True
					else:
						return None
				else: # if hunks are non-conflicting duplicates
					hunk.is_duplicate = True
		last_hunk = hunk
	# Remove hunks marked to be removed
	sorted_hunks = [hunk for hunk in sorted_hunks if not hunk.is_duplicate]
	
	# Fix the new offsets
	offset = 0
	for hunk in sorted_hunks:
		hunk.start_b = hunk.start_a + offset
		offset += hunk.len_b - hunk.len_a
	
	# Build output
	output = ""
	for line in file_a.header:
		output += line.content
	for hunk in sorted_hunks:
		output += "@@ -" + str(hunk.start_a) + "," + str(hunk.len_a) + " +" + str(hunk.start_b) + "," + str(hunk.len_b) + " @@" + hunk.suffix
		for line in hunk.lines:
			output += line.content
	
	return output

def main(files):
	for filename in files:
		with open(filename) as f:
			content = f.read()
		print("Resolving conflicts in file " + filename)
		content = process_file(content)
		if content == None:
			print("Could not resolve conflicts in file " + filename)
		else:
			with open(filename, "w") as f:
				f.write(content)
			os.system("git add " + filename)
			print("Resolved conflicts in file " + filename)

if len(sys.argv) < 2:
	print("patch_mergetool.py <files...>")
else:
	main(sys.argv[1:])
