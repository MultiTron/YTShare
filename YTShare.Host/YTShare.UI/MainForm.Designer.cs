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
        picQrCode = new PictureBox();
        lblIpAddress = new Label();
        ((System.ComponentModel.ISupportInitialize)picQrCode).BeginInit();
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
        lblIpAddress.Location = new Point(175, 18);
        lblIpAddress.Name = "lblIpAddress";
        lblIpAddress.Size = new Size(0, 15);
        lblIpAddress.TabIndex = 1;
        lblIpAddress.Click += label1_Click;
        // 
        // MainForm
        // 
        AutoScaleDimensions = new SizeF(7F, 15F);
        AutoScaleMode = AutoScaleMode.Font;
        ClientSize = new Size(384, 461);
        Controls.Add(lblIpAddress);
        Controls.Add(picQrCode);
        Name = "MainForm";
        Text = "YouTube© Share";
        Load += MainForm_Load;
        ((System.ComponentModel.ISupportInitialize)picQrCode).EndInit();
        ResumeLayout(false);
        PerformLayout();
    }

    #endregion

    private PictureBox picQrCode;
    private Label lblIpAddress;
}
