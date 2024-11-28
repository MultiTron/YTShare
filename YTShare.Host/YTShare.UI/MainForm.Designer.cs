namespace YTShare.UI;

partial class MainForm
{
    /// <summary>
    ///  Required designer variable.
    /// </summary>
    private System.ComponentModel.IContainer components = null;

    /// <summary>
    ///  Clean up any resources being used.
    /// </summary>
    /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
    protected override void Dispose(bool disposing)
    {
        if (disposing && (components != null))
        {
            components.Dispose();
        }
        base.Dispose(disposing);
    }

    #region Windows Form Designer generated code

    /// <summary>
    ///  Required method for Designer support - do not modify
    ///  the contents of this method with the code editor.
    /// </summary>
    private void InitializeComponent()
    {
        components = new System.ComponentModel.Container();
        System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(MainForm));
        picQrCode = new PictureBox();
        lblIpAddress = new Label();
        notifyIcon = new NotifyIcon(components);
        contextMenuStrip = new ContextMenuStrip(components);
        tsmiShow = new ToolStripMenuItem();
        toolStripSeparator1 = new ToolStripSeparator();
        tsmiExit = new ToolStripMenuItem();
        ((System.ComponentModel.ISupportInitialize)picQrCode).BeginInit();
        contextMenuStrip.SuspendLayout();
        SuspendLayout();
        // 
        // picQrCode
        // 
        picQrCode.Location = new Point(45, 80);
        picQrCode.Name = "picQrCode";
        picQrCode.Padding = new Padding(10, 0, 10, 10);
        picQrCode.Size = new Size(300, 300);
        picQrCode.TabIndex = 0;
        picQrCode.TabStop = false;
        picQrCode.Click += pictureBox1_Click;
        // 
        // lblIpAddress
        // 
        lblIpAddress.AutoSize = true;
        lblIpAddress.Location = new Point(142, 18);
        lblIpAddress.Name = "lblIpAddress";
        lblIpAddress.Size = new Size(0, 15);
        lblIpAddress.TabIndex = 1;
        lblIpAddress.Click += label1_Click;
        // 
        // notifyIcon
        // 
        notifyIcon.ContextMenuStrip = contextMenuStrip;
        notifyIcon.Icon = (Icon)resources.GetObject("notifyIcon.Icon");
        notifyIcon.Text = "YTShare";
        notifyIcon.Visible = true;
        // 
        // contextMenuStrip
        // 
        contextMenuStrip.Items.AddRange(new ToolStripItem[] { tsmiShow, toolStripSeparator1, tsmiExit });
        contextMenuStrip.Name = "contextMenuStrip";
        contextMenuStrip.Size = new Size(181, 76);
        // 
        // tsmiShow
        // 
        tsmiShow.Name = "tsmiShow";
        tsmiShow.Size = new Size(180, 22);
        tsmiShow.Text = "Show";
        tsmiShow.Click += tsmiShow_Click;
        // 
        // toolStripSeparator1
        // 
        toolStripSeparator1.Name = "toolStripSeparator1";
        toolStripSeparator1.Size = new Size(177, 6);
        // 
        // tsmiExit
        // 
        tsmiExit.Name = "tsmiExit";
        tsmiExit.Size = new Size(180, 22);
        tsmiExit.Text = "Exit";
        // 
        // MainForm
        // 
        AutoScaleDimensions = new SizeF(7F, 15F);
        AutoScaleMode = AutoScaleMode.Font;
        ClientSize = new Size(384, 461);
        Controls.Add(lblIpAddress);
        Controls.Add(picQrCode);
        Icon = (Icon)resources.GetObject("$this.Icon");
        Name = "MainForm";
        ShowInTaskbar = false;
        Text = "YouTube© Share";
        Load += MainForm_Load;
        ((System.ComponentModel.ISupportInitialize)picQrCode).EndInit();
        contextMenuStrip.ResumeLayout(false);
        ResumeLayout(false);
        PerformLayout();
    }

    #endregion

    private PictureBox picQrCode;
    private Label lblIpAddress;
    private NotifyIcon notifyIcon;
    private ContextMenuStrip contextMenuStrip;
    private ToolStripMenuItem tsmiShow;
    private ToolStripSeparator toolStripSeparator1;
    private ToolStripMenuItem tsmiExit;
}
