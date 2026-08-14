"""
This module provides a pre-commit hook script to prevent accidental leaks of sensitive
environment variables into the git repository. It parses a specified .env file, extracts
the values of the defined environment variables, and scans a provided list of files for
any occurrences of these sensitive values.
"""

import os
import sys

def get_env_values(env_path):
    """
    Extract sensitive values from a given .env file.

    This function reads the specified .env file line by line. It ignores comments and
    empty lines, extracts the values assigned to environment variables, removes any
    surrounding quotes or whitespace, and filters out values that are 5 characters or
    shorter to prevent false positives when scanning code.

    :param env_path: The absolute or relative path to the .env file to be parsed.
    :type env_path: str
    :return: A set containing the extracted sensitive string values.
    :rtype: set[str]
    """
    if not os.path.exists(env_path):
        return set()
    values = set()
    with open(env_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            if '=' in line:
                key, val = line.split('=', 1)
                val = val.strip(' \'"') # remove quotes and spaces
                # Only track values longer than 5 chars to avoid false positives on short generic strings
                if len(val) > 5:
                    values.add(val)
    return values

def check_files(files_to_check, sensitive_values):
    """
    Scan a list of files for any occurrences of the provided sensitive values.

    This function iterates through the provided file paths, opens each file as UTF-8
    text, and checks if any of the sensitive values are present in the file's contents.
    If a sensitive value is found, an error message is printed to standard output.
    Files that cannot be decoded as UTF-8 (e.g., binary files) are safely skipped.

    :param files_to_check: A list of file paths to be scanned for leaks.
    :type files_to_check: list[str]
    :param sensitive_values: A set of sensitive string values to search for.
    :type sensitive_values: set[str]
    :return: True if at least one sensitive value was found in any of the files, False otherwise.
    :rtype: bool
    """
    leaked = False
    for filepath in files_to_check:
        if not os.path.exists(filepath):
            continue
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
                for val in sensitive_values:
                    if val in content:
                        print(f"ERROR: Sensitive value from .env found in {filepath}!")
                        leaked = True
        except UnicodeDecodeError:
            # Skip binary files safely
            pass
    return leaked

if __name__ == '__main__':
    env_file = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '.env'))
    sensitive_values = get_env_values(env_file)
    
    if not sensitive_values:
        sys.exit(0)
        
    files_to_check = sys.argv[1:]
    if check_files(files_to_check, sensitive_values):
        print("Commit rejected: .env secrets detected.")
        sys.exit(1)
    
    sys.exit(0)
