## Language Options
- [Phiên bản Tiếng Việt](README.vn.md)

# AbyssVideoDownloader

**AbyssVideoDownloader** is a command-line Kotlin application that allows users to download Abyss.to videos. It supports downloading videos in different resolutions.

## Prerequisites

Before using AbyssVideoDownloader, ensure you have Java Development Kit (JDK 21) is installed.

### Installation Instructions for JDK 21

<details>
    <summary>Linux</summary>


To install JDK 21 on a Linux machine using the command line:

1. **Open Terminal** on your Linux machine.
2. **Update package index**:

   ```bash
   sudo apt update
   ```

3. **Install OpenJDK 21** (if available via your package manager):

   ```bash
   sudo apt install openjdk-21-jdk
   ```

   If JDK 21 is not available in your distribution’s package manager, you can manually download and install it:

    1. **Download JDK 21** from [Oracle's official site](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html).

    2. **Extract the downloaded archive** (replace `jdk-21_linux-x64_bin.tar.gz` with the actual file name):

       ```bash
       tar -xvzf jdk-21_linux-x64_bin.tar.gz
       ```

    3. **Move the JDK folder** to `/opt`:

       ```bash
       sudo mv jdk-21 /opt/
       ```

    4. **Set environment variables**:

       ```bash
       sudo update-alternatives --install /usr/bin/java java /opt/jdk-21/bin/java 1
       sudo update-alternatives --install /usr/bin/javac javac /opt/jdk-21/bin/javac 1
       ```

    5. **Verify the installation**:

       ```bash
       java -version
       ```

  </details>

<details>
    <summary>Windows</summary>


To install JDK 21 on a Windows machine:

1. **Download JDK 21** from [Oracle's official site](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html).
2. **Run the installer** and follow the on-screen instructions.
3. During the installation, make sure to select the option to **add Java to your system’s PATH**.
4. **Verify the installation**:

    - Open **Command Prompt** (`cmd`).
    - Type the following command:

      ```cmd
      java -version
      ```

If command return the version number (e.g., `java 21`), then the installation was successful.

</details>


## Getting the JAR File

You have two options for obtaining the `abyss-dl.jar` file:

1. **Download the Latest JAR File**:
   - You can download the latest `abyss-dl.jar` file from the [Releases](https://github.com/abdlhay/AbyssVideoDownloader/releases) section of this repository.

2. **Build the JAR File Manually**:
   - If you prefer to build the project yourself or want the latest updates, follow the instructions in the [**Building the Project**](#building-the-project) section below.


## Usage

### Downloading a Video with AbyssVideoDownloader

To download a video, follow these steps:

1. **Run the Command**:
   Open your terminal and enter the following command:

   ```bash
   java -jar abyss-dl.jar <id_or_url_with_resolution> [-H <header>] [--header <header>] [-o <output_file_path>] [-c <number_of_connections>]
   ```

- **Video URL or ID**:  
  Provide the video ID or URL at the beginning of the command, followed by an optional resolution (`h`, `m`, `l`).  
  Examples:
    - `K8R6OOjS7 h` (High resolution)
    - `https://abysscdn.com/?v=K8R6OOjS7 l` (Low resolution)
    - `K8R6OOjS7 m` (Medium resolution)
    - `K8R6OOjS7 h,K8R6OOjS7 m` (Multiple videos with resolutions)
- **Add HTTP Headers**:  
  Use `-H` or `--header` to include headers (e.g., `-H "Authorization: Bearer TOKEN"`). Repeat to add multiple headers.

- **Output File**:  
  Use `-o <path>` to specify the download location (e.g., `-o /path/video.mp4`). Defaults to the current directory.

- **Connections**:  
  Use `-c <1-10>` to set concurrent connections (default: 4).

- **Retries**:  
  Use `--retry <1-n>` to set the number of attempts to download a video

- **Verbose Mode**:  
  Add `--verbose` for detailed output.


### Examples

1. **Download a video and save it with a specific name in a chosen folder**:
   ```bash
   java -jar abyss-dl.jar K8R6OOjS7 -o /path/to/directory/my_video.mp4
   ```

2. **Download a video and save it with the default name in the current directory**:
   ```bash
   java -jar abyss-dl.jar K8R6OOjS7
   ```

3. **Download a video using custom headers**:
   ```bash
   java -jar abyss-dl.jar K8R6OOjS7 -H "Authorization: Bearer TOKEN" --header "Referer: https://example.com" -o my_video.mp4
   ```

4. **Download a video with multiple connections**:
   ```bash
   java -jar abyss-dl.jar K8R6OOjS7 -c 7 -o my_video.mp4
   ```

5. **Download a video using custom headers and multiple connections**:
   ```bash
   java -jar abyss-dl.jar K8R6OOjS7 -H "Authorization: Bearer TOKEN" --header "Referer: https://example.com" -c 3 -o /path/to/my_video.mp4
   ```

6. **Download a video with retries**:
   ```bash
   java -jar abyss-dl.jar K8R6OOjS7 --retry 3 -o /path/to/my_video.mp4
   ```

6. Download multiple videos with resolutions from a text file or inline:  
   **From text file:**  
   The file should list videos as:
   ```text
   id1
   id2 h
   https://example.com/video l
   ```
   ```bash
   java -jar abyss-dl.jar videos.txt
   ```  
   **Inline with comma-separated values:**
   ```bash
   java -jar abyss-dl.jar id1 h,id2 m,https://example.com/video l
   ```
## Building the Project

You can build the AbyssVideoDownloader project using GitHub Actions by following these steps:

### Steps to Build the Project

1. **Fork the Repository**:
    - Click the **"Fork"**.

2. **Open the Actions Tab**:
    - Navigate to your forked repository and click on the **"Actions"** tab.

3. **Run the Build Workflow**:
    - Find the build workflow `build AbyssVideoDownloader`, click on it, and then select **"Run workflow"**.

4. **Access the Releases Section**:
    - After the build completes, navigate to the **Releases** section of your repository. The JAR file, `abyss-dl.jar`, will be available for download there.

## TODOs

Here are the planned tasks and features for future updates:

- [ ] Clean Code.
- [x] Add support for multiple parallel downloads.
- [x] Integrate dependency injection
- [ ] Improve error handling and provide more descriptive messages for common issues.
- [x] resume logic for failed downloads.
- [x] Implement retry
- [x] Enhance logging with different verbosity levels (e.g., debug, info, error).
- [ ] Add support for proxy configuration.
- [ ] Add a graphical user interface (GUI) for easier interaction or maybe an android app

Feel free to contribute by picking any task or suggesting new ones!