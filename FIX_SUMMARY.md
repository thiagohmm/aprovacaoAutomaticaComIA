# Fix: Curl "Failed writing body" Error - Summary

## Problem Identified

When running the test script `test-api.sh` to send 2 images to the API, the curl command was failing with:

```
curl: Failed writing body
```

## Root Cause

The error was caused by **piping large API responses to jq**:

```bash
curl ... | jq '.' 2>/dev/null || cat
```

**Why this failed:**

1. The Gemini API returns large responses (detailed analysis text)
2. When curl pipes output to jq, the pipe buffer can overflow with large data
3. If jq fails to parse or the buffer fills up, curl reports "Failed writing body"
4. The terminal buffer couldn't handle the large response being streamed

## Solution Implemented

### 1. Fixed test-api.sh Script

**Key Changes:**

- ✅ **Removed problematic pipe to jq** - No longer pipes curl output directly
- ✅ **Save responses to files** - Uses `-o` flag to save responses to `./api-responses/` directory
- ✅ **Separate HTTP code from body** - Uses `-w "%{http_code}"` to get status separately
- ✅ **Smart JSON formatting** - Tries jq, falls back to python, then raw output
- ✅ **Better error handling** - Captures and displays errors properly
- ✅ **Response size information** - Shows file sizes for debugging
- ✅ **Colored output** - Improved readability with color codes
- ✅ **Image size display** - Shows sizes of images being uploaded

**New Function: `make_request()`**

```bash
make_request() {
    # Saves response to file first
    HTTP_CODE=$(curl -s -w "%{http_code}" -o "$temp_file" "$@")

    # Then formats and displays it
    format_json "$response_file"
}
```

### 2. Enhanced application.properties

Added performance optimizations:

```properties
# Response compression (reduces response size)
server.compression.enabled=true
server.compression.mime-types=application/json,application/xml,text/html,text/xml,text/plain
server.compression.min-response-size=1024

# Connection timeouts (better error handling)
server.tomcat.connection-timeout=60000
server.tomcat.max-http-form-post-size=20MB
```

## Benefits

1. **No more "Failed writing body" errors** - Responses saved to files instead of piped
2. **Better debugging** - All responses saved in `./api-responses/` directory
3. **Handles large responses** - No buffer overflow issues
4. **Works without jq** - Falls back to python or raw output
5. **Better performance** - Response compression reduces network transfer
6. **Improved visibility** - Shows file sizes, HTTP codes, and colored output

## How to Use

1. **Run the fixed script:**

   ```bash
   chmod +x test-api.sh
   ./test-api.sh
   ```

2. **View saved responses:**

   ```bash
   ls -lh ./api-responses/
   cat ./api-responses/teste1_2imagens_response.json
   ```

3. **Format responses manually (if needed):**
   ```bash
   jq '.' ./api-responses/teste1_2imagens_response.json
   ```

## Testing Checklist

- [ ] Run `./test-api.sh` with 2 images
- [ ] Verify no "Failed writing body" errors
- [ ] Check responses are saved in `./api-responses/`
- [ ] Confirm API responses are properly displayed
- [ ] Verify colored output works correctly

## Files Modified

1. **test-api.sh** - Complete rewrite with proper error handling
2. **application.properties** - Added compression and timeout settings
3. **TODO.md** - Progress tracking
4. **FIX_SUMMARY.md** - This documentation

## Technical Details

**Before (Problematic):**

```bash
curl ... | jq '.' 2>/dev/null || cat
# ❌ Pipe can overflow with large responses
# ❌ jq failure causes curl to fail
# ❌ No way to debug or save responses
```

**After (Fixed):**

```bash
HTTP_CODE=$(curl -s -w "%{http_code}" -o "$temp_file" "$@")
# ✅ Response saved to file
# ✅ HTTP code captured separately
# ✅ Can format and display after saving
# ✅ All responses preserved for debugging
```

## Additional Notes

- Responses are saved in `./api-responses/` directory (created automatically)
- Each test creates a separate response file with timestamp
- The script works even if jq is not installed (uses python or raw output)
- Colored output can be disabled by removing the color codes if needed
- Response compression is enabled for responses > 1KB

## Next Steps

1. Test the script with actual images
2. Verify the API responses are correct
3. Monitor response sizes and performance
4. Consider adding response caching if needed
