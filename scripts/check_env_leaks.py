import os
import sys

def get_env_values(env_path):
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
